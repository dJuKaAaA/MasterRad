"""
Distributed Transaction Correctness Test Suite
================================================
Tests correctness of 2PC, TCC, and Saga protocols under network failure scenarios.
Uses Toxiproxy to inject failures and PostgreSQL to verify system consistency.

Requirements:
    pip install requests psycopg2-binary

Usage:
    python correctness_tests.py                  # run all tests
    python correctness_tests.py --protocol 2pc   # run only 2PC tests
    python correctness_tests.py --protocol tcc   # run only TCC tests
    python correctness_tests.py --protocol saga  # run only Saga tests
"""

import requests
import psycopg2
import time
import argparse
import sys
from dataclasses import dataclass
from typing import Optional
from datetime import datetime
import uuid

# ─────────────────────────────────────────────
# CONFIGURATION — adjust these to your setup
# ─────────────────────────────────────────────

TOXIPROXY_HOST  = "http://localhost:8474"
API_URL    = "http://localhost:80"

# Endpoints — fill in SAGA_ENDPOINT when you remember it
ENDPOINTS = {
    "2pc":  "/api/v1/coordinator/create-order",
    "tcc":  "/api/v1/coordinator/create-order",
    "saga": "/api/v1/saga/create-order",   # <-- fill in
}

# Test transaction payload — adjust field names to match your API
TEST_PAYLOAD = {
    "productId": "d572df76-b527-4e31-8aa3-9aa954d17100",
    "amount":  1,
}

# How long to wait after a transaction for compensations/rollbacks to settle (seconds)
SETTLEMENT_TIMEOUT = 5 
SETTLEMENT_TIMEOUT_XA = 15 

USER_ID = {
        "saga": "8cca7a29-5add-4197-ad56-48be327ea13c",
        "2pc": "daa45fd6-3500-4a0d-914d-052082303122",
        "tcc": "19e5ddb1-4c66-4d17-ad06-e8a6af23ed58"
        }

# PostgreSQL connections — adjust host/port/credentials per service
DB_CONFIG = {
    "saga": {
        "order": {
            "host":     "localhost",
            "port":     5433,
            "dbname":   "saga_order",        # adjust
            "user":     "order",       # adjust
            "password": "order",       # adjust
        },
        "inventory": {
            "host":     "localhost",
            "port":     5434,             # adjust if different
            "dbname":   "saga_inventory",    # adjust
            "user":     "inventory",
            "password": "inventory",
        },
        "payment": {
            "host":     "localhost",
            "port":     5435,             # adjust if different
            "dbname":   "saga_payment",      # adjust
            "user":     "payment",
            "password": "payment",
        },
        "saga-orchestrator": {
            "host":     "localhost",
            "port":     5436,             # adjust if different
            "dbname":   "saga_orchestrator",      # adjust
            "user":     "orchestrator",
            "password": "orchestrator",
        },
    },
    "tcc": {
        "order": {
            "host":     "localhost",
            "port":     5433,
            "dbname":   "tcc_order",        # adjust
            "user":     "order",       # adjust
            "password": "order",       # adjust
        },
        "inventory": {
            "host":     "localhost",
            "port":     5434,             # adjust if different
            "dbname":   "tcc_inventory",    # adjust
            "user":     "inventory",
            "password": "inventory",
        },
        "payment": {
            "host":     "localhost",
            "port":     5435,             # adjust if different
            "dbname":   "tcc_payment",      # adjust
            "user":     "payment",
            "password": "payment",
        },
        "coordinator": {
            "host":     "localhost",
            "port":     5436,             # adjust if different
            "dbname":   "coordinator",      # adjust
            "user":     "coordinator",
            "password": "coordinator",
        },
    },
    "2pc": {
        "order": {
            "host":     "localhost",
            "port":     5433,
            "dbname":   "2pc_order",        # adjust
            "user":     "order",       # adjust
            "password": "order",       # adjust
        },
        "inventory": {
            "host":     "localhost",
            "port":     5434,             # adjust if different
            "dbname":   "2pc_inventory",    # adjust
            "user":     "inventory",
            "password": "inventory",
        },
        "payment": {
            "host":     "localhost",
            "port":     5435,             # adjust if different
            "dbname":   "2pc_payment",      # adjust
            "user":     "payment",
            "password": "payment",
        },
        "coordinator": {
            "host":     "localhost",
            "port":     5436,             # adjust if different
            "dbname":   "coordinator",      # adjust
            "user":     "coordinator",
            "password": "coordinator",
        },

    }

}

# Toxiproxy proxy names — must match what you created with toxiproxy-cli
PROXIES = {
    "order":     "order-service",
    "inventory": "inventory-service",
    "payment":   "payment-service",
}

DB_PROXIES = {
    "order":     "order-db",
    "inventory": "inventory-db",
    "payment":   "payment-db",
}

# Table/column names — adjust to match your actual schema
# These are used to verify consistency after each test
SCHEMA = {
    "order": {
        "table":        "order_entity",
        "user_col":     "user_id",
        "status_col":   "status",
        "confirmed":    "COMPLETE",   # value when committed
        "cancelled":    "CANCELED",   # value when rolled back
    },
    "inventory": {
        "table":        "inventory_entity",
        "product_col":  "product_id",
        "quantity_col": "available_stock",
        "initial_qty":  1000000,           # quantity before test transaction
    },
    "payment": {
        "table":        "payment_entity",
        "user_col":     "payer_id",
        "status_col":   "status",
        "confirmed":    "SUCCESS",      # value when committed
        "cancelled":    "FAILED",       # value when rolled back
    },
    "coordinator": {
        "table": "coordinator_transaction_entity"
        },
    "saga-orchestrator": {
        "table": "saga_entity"
        }
}

# ─────────────────────────────────────────────
# AUTH
# ─────────────────────────────────────────────

LOGIN_URL = "http://localhost:80/api/v1/identity-provider/login"
LOGIN_PAYLOAD = {
    "username": "pera123",
    "password": "Pera123",
}

def get_token() -> str:
    """Authenticate and return a Bearer token."""
    res = requests.post(
        LOGIN_URL,
        json=LOGIN_PAYLOAD,
        timeout=10,
    )
    if res.status_code != 200:
        raise Exception(f"Login failed: {res.status_code} — {res.text}")

    data = res.json()

    # Adjust the key below if your response uses a different field name
    # Common ones: "access_token", "token", "accessToken"
    token = data.get("token")

    if not token:
        raise Exception(f"Token not found in login response: {data}")

    print(f"  Auth     : token acquired")
    return token

# ─────────────────────────────────────────────
# RESULT TRACKING
# ─────────────────────────────────────────────

@dataclass
class TestResult:
    name:        str
    protocol:    str
    failure:     str
    consistent:  Optional[bool] = None
    outcome:     str = ""
    http_status: Optional[int] = None
    error:       str = ""
    duration_ms: float = 0.0

results: list[TestResult] = []

# ─────────────────────────────────────────────
# DB HELPERS
# ─────────────────────────────────────────────

def get_conn(protocol: str, service: str):
    return psycopg2.connect(**DB_CONFIG[protocol][service])

def reset_state(protocol: str):
    """Clean test data from all databases before each test."""
    product = TEST_PAYLOAD["productId"]
    qty     = SCHEMA["inventory"]["initial_qty"]

    with get_conn(protocol, "order") as conn:
        with conn.cursor() as cur:
            cur.execute(
                f"DELETE FROM {SCHEMA['order']['table']}"
            )

    with get_conn(protocol, "inventory") as conn:
        with conn.cursor() as cur:
            cur.execute(
                f"UPDATE {SCHEMA['inventory']['table']} SET {SCHEMA['inventory']['quantity_col']} = %s  WHERE {SCHEMA['inventory']['product_col']} = %s",
                (qty, product)
            )

    with get_conn(protocol, "payment") as conn:
        with conn.cursor() as cur:
            cur.execute(
                f"DELETE FROM {SCHEMA['payment']['table']}",
            )

    coordinator = "saga-orchestrator" if protocol == "saga" else "coordinator"
    with get_conn(protocol, coordinator) as conn:
        with conn.cursor() as cur:
            table = SCHEMA[coordinator]['table']
            cur.execute(
                f"DELETE FROM {table}",
            )

    with get_conn(protocol, coordinator) as conn:
        with conn.cursor() as cur:
            cur.execute(
                f"DELETE FROM participant_data_entity",
            )

def check_consistency(protocol: str) -> tuple[bool, str, dict]:
    """
    Query all three databases and determine if the system is in a consistent state.
    Returns (is_consistent, message, raw_state).
    """
    user    = USER_ID[protocol]
    product = TEST_PAYLOAD["productId"]
    payer_id = "d1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20"
    payment_status = None



    with get_conn(protocol, "payment") as conn:
        with conn.cursor() as cur:
            cur.execute(
                f"SELECT id, status, coordinator_payment_id FROM {SCHEMA['payment']['table']} "
                f"WHERE {SCHEMA['payment']['user_col']} = %s",
                (payer_id,)
            )
            print()
            print("MI BOMBO:", cur.fetchone())
            print()



    with get_conn(protocol, "order") as conn:
        with conn.cursor() as cur:
            cur.execute(
                f"SELECT {SCHEMA['order']['status_col']} FROM {SCHEMA['order']['table']} "
                f"WHERE {SCHEMA['order']['user_col']} = %s ORDER BY created_at DESC LIMIT 1",
                (user,)
            )
            row = cur.fetchone()
            order_status = row[0] if row else None

    with get_conn(protocol, "inventory") as conn:
        with conn.cursor() as cur:
            cur.execute(
                f"SELECT {SCHEMA['inventory']['quantity_col']} FROM {SCHEMA['inventory']['table']} "
                f"WHERE {SCHEMA['inventory']['product_col']} = %s",
                (product,)
            )
            row = cur.fetchone()
            inventory_qty = row[0] if row else None

    with get_conn(protocol, "payment") as conn:
        with conn.cursor() as cur:
            cur.execute(
                f"SELECT COUNT(*) FROM {SCHEMA['payment']['table']} "
                f"WHERE {SCHEMA['payment']['user_col']} = %s",
                (payer_id,)
            )
            payment_count = cur.fetchone()[0]

    if payment_count > 0:
        with get_conn(protocol, "payment") as conn:
            with conn.cursor() as cur:
                cur.execute(
                    f"SELECT {SCHEMA['payment']['status_col']} FROM {SCHEMA['payment']['table']} "
                    f"WHERE {SCHEMA['payment']['user_col']} = %s",
                    (payer_id,)
                )
                payment_status = cur.fetchone()[0]

    state = {
        "order_status":    order_status,
        "inventory_qty":   inventory_qty,
        "payment_count":   payment_count,
        "payment_status":  payment_status,
    }

    initial_qty   = SCHEMA["inventory"]["initial_qty"]
    confirmed_val = SCHEMA["order"]["confirmed"]
    cancelled_val = SCHEMA["order"]["cancelled"]
    payment_success = SCHEMA["payment"]["confirmed"]
    payment_failed = SCHEMA["payment"]["cancelled"]

    # Fully committed: order confirmed, inventory reduced by 1, payment exists
    if (order_status == confirmed_val
            and inventory_qty == initial_qty - TEST_PAYLOAD["amount"]
            and payment_count == 1 and payment_status is not None and payment_status == payment_success):
        return True, "CONSISTENT — fully committed", state

    # Fully rolled back: order cancelled or absent, inventory unchanged, no payment
    if (order_status in (cancelled_val, None)
            and inventory_qty == initial_qty
            and (payment_count == 0 or (payment_count == 1 and payment_status == payment_failed))):
        return True, "CONSISTENT — fully rolled back", state

    # Anything else is inconsistent
    return False, (
        f"INCONSISTENT — order={order_status}, "
        f"inventory={inventory_qty} (expected {initial_qty} or {initial_qty - 1}), "
        f"payments={payment_count}, "
        f"payment_status={payment_status}"
    ), state

# ─────────────────────────────────────────────
# TOXIPROXY HELPERS
# ─────────────────────────────────────────────

def add_toxic(proxy: str, name: str, toxic_type: str, attributes: dict, direction: str = "downstream"):
    requests.post(
        f"{TOXIPROXY_HOST}/proxies/{proxy}/toxics",
        json={"name": name, "type": toxic_type, "stream": direction, "attributes": attributes},
        timeout=5,
    )

def remove_toxic(proxy: str, name: str):
    try:
        requests.delete(f"{TOXIPROXY_HOST}/proxies/{proxy}/toxics/{name}", timeout=5)
    except Exception:
        pass  # already removed or never added

def disable_proxy(proxy: str):
    requests.post(f"{TOXIPROXY_HOST}/proxies/{proxy}/disable", timeout=5)

def enable_proxy(proxy: str):
    requests.post(f"{TOXIPROXY_HOST}/proxies/{proxy}/enable", timeout=5)

def cleanup_all_toxics(protocol: str):
    """Remove all toxics from all proxies — call between tests to ensure clean state."""
    if protocol == "tcc":
        for proxy in PROXIES.values():
            try:
                r = requests.get(f"{TOXIPROXY_HOST}/proxies/{proxy}/toxics", timeout=5)
                for toxic in r.json():
                    remove_toxic(proxy, toxic["name"])
                enable_proxy(proxy)
            except Exception:
                pass

    if protocol == "saga":
        for proxy in ["kafka"]:
            try:
                r = requests.get(f"{TOXIPROXY_HOST}/proxies/{proxy}/toxics", timeout=5)
                for toxic in r.json():
                    remove_toxic(proxy, toxic["name"])
                enable_proxy(proxy)
            except Exception:
                pass

    if protocol == "2pc":
        for proxy in DB_PROXIES.values():
            try:
                r = requests.get(f"{TOXIPROXY_HOST}/proxies/{proxy}/toxics", timeout=5)
                for toxic in r.json():
                    remove_toxic(proxy, toxic["name"])
                enable_proxy(proxy)
            except Exception:
                pass


# ─────────────────────────────────────────────
# TRANSACTION RUNNER
# ─────────────────────────────────────────────

def run_transaction(protocol: str, token: str) -> tuple[Optional[int], float]:
    """Fire one transaction. Returns (http_status, duration_ms). Status is None on timeout."""
    endpoint = ENDPOINTS[protocol]
    start = time.time()
    try:
        res = requests.post(
            f"{API_URL}{endpoint}",
            json=TEST_PAYLOAD,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {token}",
                "Idempotency-Key": f"{uuid.uuid4()}"
            },
            timeout=30,
        )
        duration_ms = (time.time() - start) * 1000
        return res.status_code, duration_ms
    except requests.exceptions.Timeout:
        duration_ms = (time.time() - start) * 1000
        return None, duration_ms
    except requests.exceptions.ConnectionError as _:
        duration_ms = (time.time() - start) * 1000
        return None, duration_ms

# ─────────────────────────────────────────────
# CORE TEST RUNNER
# ─────────────────────────────────────────────

def run_test(
    name: str,
    protocol: str,
    failure_description: str,
    inject_fn,           # callable: injects toxics
    cleanup_fn,          # callable: removes toxics
    settlement: int = SETTLEMENT_TIMEOUT,
    pre_delay: float = 0,
) -> TestResult:
    """
    Run a single correctness test.
    inject_fn is called before the transaction.
    cleanup_fn is called after settlement timeout.
    pre_delay: seconds to wait after injecting before firing the transaction.
    """
    result = TestResult(name=name, protocol=protocol, failure=failure_description)
    sep = "─" * 55
    print(f"\n{sep}")
    print(f"  {name}")
    print(f"  Protocol : {protocol.upper()}")
    print(f"  Failure  : {failure_description}")
    print(sep)

    try:
        reset_state(protocol)
        cleanup_all_toxics(protocol)
        inject_fn()

        if pre_delay:
            time.sleep(pre_delay)

        token = get_token()
        status, duration_ms = run_transaction(protocol, token)
        result.http_status = status
        result.duration_ms = duration_ms

        status_str = str(status) if status else "TIMEOUT"
        print(f"  HTTP     : {status_str}  ({duration_ms:.0f}ms)")

        print(f"  Settling : waiting {settlement}s for compensations...")
        time.sleep(settlement)

        cleanup_fn()

        # Give one more second after toxic removal for any retries to complete
        time.sleep(1)

        consistent, message, state = check_consistency(protocol)
        result.consistent = consistent
        result.outcome = message

        print(f"  State    : order={state['order_status']}, "
              f"inventory={state['inventory_qty']}, "
              f"payments={state['payment_count']}, "
              f"payment_status={state['payment_status']}")
        icon = "✅" if consistent else "❌"
        print(f"  Result   : {icon} {message}")

    except Exception as e:
        result.consistent = False
        result.error = str(e)
        result.outcome = f"ERROR: {e}"
        print(f"  ERROR    : {e}")
    finally:
        cleanup_all_toxics(protocol)

    results.append(result)
    return result

# ─────────────────────────────────────────────
# TEST CASES
# ─────────────────────────────────────────────

def test_baseline(protocol: str):
    """TC-00: Happy path — no failure. All protocols should be consistent."""
    run_test(
        name=f"TC-00 Baseline — happy path",
        protocol=protocol,
        failure_description="No failure injected",
        inject_fn=lambda: None,
        cleanup_fn=lambda: None,
    )

# ── SAGA ─────────────────────────────────────

def test_saga_order_service_down():
    """
    TC-S1: Order service stops consuming from Kafka at the very first step.
    Saga never progresses — nothing should be modified anywhere.
    Expected: CONSISTENT (rolled back or never started).
    """
    import subprocess

    def inject():
        subprocess.run(["docker", "stop", "saga-order-service"])
        subprocess.run(["docker", "wait", "saga-order-service"])

    def cleanup():
        subprocess.run(["docker", "start", "saga-order-service"])
        print("  Recovery : waiting 5s for order-service to restart...")
        time.sleep(5)

    run_test(
        name="TC-S1 Saga — order service down (stops consuming Kafka)",
        protocol="saga",
        failure_description="saga-order-service container stopped",
        inject_fn=inject,
        cleanup_fn=cleanup,
        settlement=10,
    )

def test_saga_inventory_service_down():
    """
    TC-S2: Inventory service stops consuming from Kafka.
    Order step may have already completed.
    Orchestrator should timeout and trigger compensation on order.
    Payment should never be charged.
    Expected: CONSISTENT (rolled back).
    """
    import subprocess

    def inject():
        subprocess.run(["docker", "stop", "saga-inventory-service"])
        subprocess.run(["docker", "wait", "saga-inventory-service"])

    def cleanup():
        subprocess.run(["docker", "start", "saga-inventory-service"])
        print("  Recovery : waiting 5s for inventory-service to restart...")
        time.sleep(5)

    run_test(
        name="TC-S2 Saga — inventory service down (stops consuming Kafka)",
        protocol="saga",
        failure_description="saga-inventory-service container stopped",
        inject_fn=inject,
        cleanup_fn=cleanup,
        settlement=10,
    )

def test_saga_payment_service_down():
    """
    TC-S3: Payment service stops consuming from Kafka.
    Order and inventory steps have already completed.
    This is the critical Saga case — inventory is already modified.
    Compensating transactions must fire to release inventory and cancel order.
    Expected: CONSISTENT (rolled back).
    """
    import subprocess

    def inject():
        subprocess.run(["docker", "stop", "saga-payment-service"])
        subprocess.run(["docker", "wait", "saga-payment-service"])

    def cleanup():
        subprocess.run(["docker", "start", "saga-payment-service"])
        print("  Recovery : waiting 5s for payment-service to restart...")
        time.sleep(5)

    run_test(
        name="TC-S3 Saga — payment service down (stops consuming Kafka)",
        protocol="saga",
        failure_description="saga-payment-service container stopped",
        inject_fn=inject,
        cleanup_fn=cleanup,
        settlement=10,
    )

def test_saga_payment_service_slow():
    """
    TC-S4: Kafka is slow delivering to payment service (4s latency).
    Tests whether orchestrator times out prematurely and triggers
    a false compensation on a transaction that would have succeeded.
    Expected: CONSISTENT (either committed or rolled back, not mixed).
    """
    run_test(
        name="TC-S4 Saga — Kafka slow to payment service (4s latency)",
        protocol="saga",
        failure_description="kafka proxy 4000ms latency — payment step delayed",
        inject_fn=lambda: add_toxic("kafka", "slow", "latency", {"latency": 4000, "jitter": 500}),
        cleanup_fn=lambda: remove_toxic("kafka", "slow"),
        settlement=15,
    )

def test_saga_payment_service_down_compensation_fails():
    """
    TC-S5: Payment service down AND inventory service slow during compensation.
    Tests whether the Saga retries compensating transactions when
    the compensation step itself is delayed.
    Expected: CONSISTENT (rolled back) — but may take longer to settle.
    """
    import subprocess

    def inject():
        subprocess.run(["docker", "stop", "saga-payment-service"])
        subprocess.run(["docker", "wait", "saga-payment-service"])
        add_toxic("kafka", "slow_inv", "latency", {"latency": 8000})

    def cleanup():
        remove_toxic("kafka", "slow_inv")
        subprocess.run(["docker", "start", "saga-payment-service"])
        print("  Recovery : waiting 5s for payment-service to restart...")
        time.sleep(5)

    run_test(
        name="TC-S5 Saga — payment down + Kafka slow during compensation",
        protocol="saga",
        failure_description="saga-payment-service stopped + kafka 8s latency during compensation",
        inject_fn=inject,
        cleanup_fn=cleanup,
        settlement=20,
    )

def test_saga_inventory_service_down_compensation_fails():
    """
    TC-S6: Inventory service goes down during the compensation step.
    Payment failed, orchestrator tries to compensate inventory —
    but inventory service is also down and stops consuming.
    Tests whether compensation is retried when the service comes back.
    Expected: CONSISTENT (rolled back) after inventory service restarts.
    """
    import subprocess

    def inject():
        subprocess.run(["docker", "stop", "saga-payment-service"])
        subprocess.run(["docker", "wait", "saga-payment-service"])

    def cleanup_fn_inner():
        subprocess.run(["docker", "stop", "saga-inventory-service"])
        print("  Injecting : stopping inventory during compensation...")
        time.sleep(2)
        subprocess.run(["docker", "start", "saga-inventory-service"])
        subprocess.run(["docker", "start", "saga-payment-service"])
        print("  Recovery : waiting 5s for services to restart...")
        time.sleep(5)

    run_test(
        name="TC-S6 Saga — inventory down during compensation (retry of compensation)",
        protocol="saga",
        failure_description="saga-payment-service stopped, saga-inventory-service stopped during compensation",
        inject_fn=inject,
        cleanup_fn=cleanup_fn_inner,
        settlement=15,
    )

def test_saga_kafka_unreachable():
    """
    TC-S7: Kafka becomes completely unreachable.
    Orchestrator sends a command but no service ever receives it.
    Does the orchestrator timeout and compensate correctly?
    Expected: CONSISTENT (rolled back).
    """
    run_test(
        name="TC-S7 Saga — Kafka unreachable",
        protocol="saga",
        failure_description="kafka proxy timeout — no messages delivered",
        inject_fn=lambda: add_toxic("kafka", "hang", "timeout", {"timeout": 0}),
        cleanup_fn=lambda: remove_toxic("kafka", "hang"),
    )

def test_saga_kafka_slow():
    """
    TC-S8: Kafka is slow (4s latency).
    Tests whether orchestrator timeouts are tuned correctly —
    slow Kafka should not trigger false compensations.
    Expected: CONSISTENT (either committed or rolled back, not mixed).
    """
    run_test(
        name="TC-S8 Saga — Kafka slow (4s latency)",
        protocol="saga",
        failure_description="kafka proxy 4000ms latency",
        inject_fn=lambda: add_toxic("kafka", "slow", "latency", {"latency": 4000, "jitter": 500}),
        cleanup_fn=lambda: remove_toxic("kafka", "slow"),
    )

def test_saga_orchestrator_crashes():
    """
    TC-S9: Saga orchestrator crashes mid-saga.
    Orchestrator has started the saga but crashes before it completes.
    Does the system recover when orchestrator restarts?
    Or is the saga left stuck in-progress forever?

    Expected: CONSISTENT after restart (committed or rolled back).
    If stuck: your orchestrator does not persist saga state to DB before acting.
    That is itself a valid thesis finding.

    Note: uses docker stop/start, not Toxiproxy.
    Timing: inject_fn waits 1s to let the saga start before killing the orchestrator.
    Adjust the sleep if your first saga step is faster or slower than 1s.
    """
    import subprocess

    def inject():
        time.sleep(1)  # let saga start, then crash
        subprocess.Popen(["docker", "stop", "saga-orchestrator"])
        subprocess.Popen(["docker", "wait", "saga-orchestrator"])

    def cleanup():
        subprocess.run(["docker", "start", "saga-orchestrator"])
        print("  Recovery : waiting 10s for orchestrator to restart...")
        time.sleep(10)

    run_test(
        name="TC-S9 Saga — orchestrator crashes mid-saga",
        protocol="saga",
        failure_description="saga-orchestrator container stopped mid-saga",
        inject_fn=inject,
        cleanup_fn=cleanup,
        settlement=15,
    )

# ── 2PC ──────────────────────────────────────

def test_2pc_order_db_hangs_prepare():
    """
    TC-2PC7: Order DB unreachable during XA PREPARE phase.
    XA coordinator cannot prepare order — must abort all participants.
    Inventory and payment DBs must be fully rolled back.
    Expected: CONSISTENT (rolled back).
    """
    run_test(
        name="TC-2PC7 2PC/XA — order DB hangs during PREPARE",
        protocol="2pc",
        failure_description="order-db timeout during XA prepare phase",
        inject_fn=lambda: add_toxic(DB_PROXIES["order"], "hang", "timeout", {"timeout": 0}),
        cleanup_fn=lambda: remove_toxic(DB_PROXIES["order"], "hang"),
    )

def test_2pc_inventory_db_hangs_prepare():
    """
    TC-2PC1: Inventory DB unreachable during XA PREPARE phase.
    XA coordinator cannot get inventory to vote — must abort all participants.
    Order and payment DBs must be fully rolled back.
    Expected: CONSISTENT (rolled back).
    """
    run_test(
        name="TC-2PC1 2PC/XA — inventory DB hangs during PREPARE",
        protocol="2pc",
        failure_description="inventory-db timeout during XA prepare phase",
        inject_fn=lambda: add_toxic(DB_PROXIES["inventory"], "hang", "timeout", {"timeout": 0}),
        cleanup_fn=lambda: remove_toxic(DB_PROXIES["inventory"], "hang"),
    )

def test_2pc_payment_db_hangs_prepare():
    """
    TC-2PC2: Payment DB unreachable during XA PREPARE phase.
    XA coordinator should abort — order and inventory must be fully rolled back.
    Expected: CONSISTENT (rolled back).
    """
    run_test(
        name="TC-2PC2 2PC/XA — payment DB hangs during PREPARE",
        protocol="2pc",
        failure_description="payment-db timeout during XA prepare phase",
        inject_fn=lambda: add_toxic(DB_PROXIES["payment"], "hang", "timeout", {"timeout": 0}),
        cleanup_fn=lambda: remove_toxic(DB_PROXIES["payment"], "hang"),
    )

def test_2pc_payment_db_hangs_commit_recovers():
    """
    TC-2PC3: Payment DB hangs during COMMIT — then comes back (XA recovery test).
    All DBs voted YES. XA writes COMMIT to log, commits order and inventory,
    then payment DB hangs. Toxic is removed after settlement so XA recovery
    can retry COMMIT to payment DB.
    Expected: CONSISTENT (fully committed) after recovery.
    If INCONSISTENT: check quarkus.transaction-manager.enable-recovery=true
    """
    run_test(
        name="TC-2PC3 2PC/XA — payment DB hangs during COMMIT then recovers",
        protocol="2pc",
        failure_description="payment-db timeout during commit — removed after settlement for XA recovery",
        inject_fn=lambda: add_toxic(DB_PROXIES["payment"], "hang", "timeout", {"timeout": 0}),
        cleanup_fn=lambda: remove_toxic(DB_PROXIES["payment"], "hang"),
        settlement=SETTLEMENT_TIMEOUT_XA,
    )

def test_2pc_payment_db_tcp_reset_commit():
    """
    TC-2PC4: TCP reset on payment DB during COMMIT.
    Connection dies mid-write. XA has already logged the commit decision.
    Does XA retry and is the result idempotent on payment DB?
    Expected: CONSISTENT (fully committed after retry).
    """
    run_test(
        name="TC-2PC4 2PC/XA — TCP reset on payment DB during COMMIT",
        protocol="2pc",
        failure_description="payment-db TCP reset after 50ms during commit",
        inject_fn=lambda: add_toxic(DB_PROXIES["payment"], "reset", "reset_peer", {"timeout": 50}),
        cleanup_fn=lambda: remove_toxic(DB_PROXIES["payment"], "reset"),
    )

def test_2pc_slow_db_false_abort():
    """
    TC-2PC5: Payment DB slow (3s latency) during PREPARE.
    Tests whether your XA timeout config is too aggressive —
    a slow-but-alive DB should not cause a false abort.
    If INCONSISTENT: tune quarkus.transaction-manager.default-timeout
    """
    run_test(
        name="TC-2PC5 2PC/XA — payment DB slow 3s during PREPARE (false abort risk)",
        protocol="2pc",
        failure_description="payment-db 3000ms latency — tests XA timeout configuration",
        inject_fn=lambda: add_toxic(DB_PROXIES["payment"], "slow", "latency", {"latency": 3000, "jitter": 300}),
        cleanup_fn=lambda: remove_toxic(DB_PROXIES["payment"], "slow"),
    )

def test_2pc_inventory_db_hangs_commit_recovers():
    """
    TC-2PC6: Inventory DB hangs during COMMIT — then comes back (partial commit recovery).
    XA committed order and payment DBs successfully, inventory DB hangs.
    Toxic removed after settlement — XA recovery must retry COMMIT to inventory.
    Expected: CONSISTENT (fully committed) after recovery.
    If INCONSISTENT: payment charged but inventory not reduced — critical inconsistency.
    """
    run_test(
        name="TC-2PC6 2PC/XA — inventory DB hangs during COMMIT then recovers",
        protocol="2pc",
        failure_description="inventory-db timeout during commit — removed after settlement for XA recovery",
        inject_fn=lambda: add_toxic(DB_PROXIES["inventory"], "hang", "timeout", {"timeout": 0}),
        cleanup_fn=lambda: remove_toxic(DB_PROXIES["inventory"], "hang"),
        settlement=SETTLEMENT_TIMEOUT_XA,
    )

# ── TCC ──────────────────────────────────────

def test_tcc_order_hangs_try():
    """
    TC-TCC0: Order service hangs during TRY phase.
    This is the very first step — nothing should have been reserved yet.
    No CANCEL needs to fire since inventory and payment were never reached.
    Expected: CONSISTENT (rolled back / never started).
    """
    run_test(
        name="TC-TCC0 TCC — order hangs during TRY (first step)",
        protocol="tcc",
        failure_description="order-service timeout during try phase",
        inject_fn=lambda: add_toxic(PROXIES["order"], "hang", "timeout", {"timeout": 0}),
        cleanup_fn=lambda: remove_toxic(PROXIES["order"], "hang"),
    )

def test_tcc_inventory_hangs_try():
    """
    TC-TCC1: Inventory hangs during TRY phase.
    Nothing should have been reserved. Cancel must fire on order.
    """
    run_test(
        name="TC-TCC1 TCC — inventory hangs during TRY",
        protocol="tcc",
        failure_description="inventory-service timeout during try phase",
        inject_fn=lambda: add_toxic(PROXIES["inventory"], "hang", "timeout", {"timeout": 0}),
        cleanup_fn=lambda: remove_toxic(PROXIES["inventory"], "hang"),
    )

def test_tcc_payment_hangs_try():
    """
    TC-TCC2: Payment hangs during TRY phase.
    Order and inventory have already reserved resources.
    Cancel must fire on both and release reservations.
    """
    run_test(
        name="TC-TCC2 TCC — payment hangs during TRY (cancel must fire on order + inventory)",
        protocol="tcc",
        failure_description="payment-service timeout during try phase",
        inject_fn=lambda: add_toxic(PROXIES["payment"], "hang", "timeout", {"timeout": 0}),
        cleanup_fn=lambda: remove_toxic(PROXIES["payment"], "hang"),
    )

def test_tcc_payment_hangs_confirm():
    """
    TC-TCC3: All TRY phases succeed, then payment hangs during CONFIRM.
    Resources are reserved on all services.
    Cancel must fire and release all reservations.
    This tests that Cancel is idempotent and retried until it succeeds.
    """
    run_test(
        name="TC-TCC3 TCC — payment hangs during CONFIRM (cancel must unwind all)",
        protocol="tcc",
        failure_description="payment-service timeout during confirm phase",
        inject_fn=lambda: add_toxic(PROXIES["payment"], "hang", "timeout", {"timeout": 0}),
        cleanup_fn=lambda: remove_toxic(PROXIES["payment"], "hang"),
    )

def test_tcc_inventory_hangs_cancel():
    """
    TC-TCC4: Payment fails during CONFIRM, then inventory hangs during CANCEL.
    Tests whether your TCC retries Cancel until it succeeds.
    Inventory reservation must eventually be released.
    """
    def inject():
        add_toxic(PROXIES["payment"], "hang_pay", "timeout", {"timeout": 0})
        add_toxic(PROXIES["inventory"], "hang_inv", "timeout", {"timeout": 0})

    def cleanup():
        remove_toxic(PROXIES["payment"], "hang_pay")
        remove_toxic(PROXIES["inventory"], "hang_inv")

    run_test(
        name="TC-TCC4 TCC — Cancel phase fails on inventory (retry of Cancel)",
        protocol="tcc",
        failure_description="payment timeout in confirm + inventory timeout in cancel",
        inject_fn=inject,
        cleanup_fn=cleanup,
    )

def test_tcc_slow_try_reservation_expiry():
    """
    TC-TCC5: TRY phase is very slow — reservation TTL may expire before CONFIRM arrives.
    Tests whether your TTL-based auto-cancel fires and leaves the system consistent.
    """
    run_test(
        name="TC-TCC5 TCC — slow TRY causes reservation TTL expiry",
        protocol="tcc",
        failure_description="inventory-service 4s latency during try — TTL may expire",
        inject_fn=lambda: add_toxic(PROXIES["inventory"], "slow", "latency", {"latency": 4000, "jitter": 500}),
        cleanup_fn=lambda: remove_toxic(PROXIES["inventory"], "slow"),
    )


def test_tcc_coordinator_crashes_during_try():
    """
    TC-TCC7: Coordinator crashes during TRY phase.
    Some services may have reserved resources, others not yet.
    No CONFIRM or CANCEL is ever sent.
    Resources reserved so far rely entirely on TTL expiry to be released.
    Expected: CONSISTENT (rolled back via TTL) — but takes time.
    Settlement is set high to exceed your TCC reservation TTL.
    Adjust settlement to be longer than your TTL if needed.
    """
    import subprocess

    def inject():
        # Kill immediately — crash happens during TRY
        subprocess.Popen(["docker", "stop", "tcc-coordinator"])

    def cleanup():
        subprocess.run(["docker", "start", "tcc-coordinator"])
        print("  Recovery : waiting 10s for coordinator to restart...")
        time.sleep(10)

    run_test(
        name="TC-TCC7 TCC — coordinator crashes during TRY phase",
        protocol="tcc",
        failure_description="tcc-coordinator stopped during try — reservations rely on TTL",
        inject_fn=inject,
        cleanup_fn=cleanup,
        settlement=30,  # must exceed your TCC reservation TTL
    )

def test_tcc_coordinator_crashes_during_confirm():
    """
    TC-TCC8: Coordinator crashes during CONFIRM phase.
    TRY succeeded on all services — resources are reserved everywhere.
    Coordinator crashes before sending CONFIRM to all services.
    Two possible correct outcomes:
      1. Coordinator restarts and resumes CONFIRM (if it persists state to DB)
      2. Reservations expire via TTL and everything rolls back
    Expected: CONSISTENT — either fully committed or fully rolled back.
    If partially confirmed: coordinator does not persist state and TTL is too long.
    """
    import subprocess

    def inject():
        time.sleep(1)  # let TRY phase complete, then crash during CONFIRM
        subprocess.Popen(["docker", "stop", "tcc-coordinator"])

    def cleanup():
        subprocess.run(["docker", "start", "tcc-coordinator"])
        print("  Recovery : waiting 10s for coordinator to restart...")
        time.sleep(10)

    run_test(
        name="TC-TCC8 TCC — coordinator crashes during CONFIRM phase",
        protocol="tcc",
        failure_description="tcc-coordinator stopped during confirm — recovery or TTL expiry",
        inject_fn=inject,
        cleanup_fn=cleanup,
        settlement=30,  # must exceed your TCC reservation TTL
    )

# ─────────────────────────────────────────────
# RESULTS SUMMARY
# ─────────────────────────────────────────────

def print_summary(active_protocol: str):
    print("\n")
    print("=" * 70)
    print("  CORRECTNESS TEST RESULTS SUMMARY")
    print(f"  Run at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 70)

    consistent_count   = sum(1 for r in results if r.consistent is True)
    inconsistent_count = sum(1 for r in results if r.consistent is False)
    error_count        = sum(1 for r in results if r.error)

    # Group by protocol
    for protocol in ["saga", "2pc", "tcc"]:
        proto_results = [r for r in results if r.protocol == protocol]
        if not proto_results:
            continue

        print(f"\n  {protocol.upper()}")
        print(f"  {'─' * 65}")
        for r in proto_results:
            if r.error:
                icon = "💥"
            elif r.consistent:
                icon = "✅"
            else:
                icon = "❌"

            http_str = str(r.http_status) if r.http_status else "TIMEOUT"
            print(f"  {icon}  [{http_str:>7}]  {r.name}")
            print(f"           {r.outcome}")

    print(f"\n  {'─' * 65}")
    print(f"  Total : {len(results)}  |  "
          f"✅ Consistent: {consistent_count}  |  "
          f"❌ Inconsistent: {inconsistent_count}  |  "
          f"💥 Errors: {error_count}")
    print("=" * 70)

    # CSV export for thesis
    csv_path = f"{active_protocol}_results.csv"
    with open(csv_path, "w") as f:
        f.write("test_name,protocol,failure,consistent,outcome,http_status,duration_ms\n")
        for r in results:
            f.write(f'"{r.name}",{r.protocol},"{r.failure}",'
                    f'{r.consistent},"{r.outcome}",'
                    f'{r.http_status or "TIMEOUT"},{r.duration_ms:.0f}\n')
    print(f"\n  Results exported to: {csv_path}")

# ─────────────────────────────────────────────
# ENTRY POINT
# ─────────────────────────────────────────────

def run_saga_tests():
    test_baseline("saga")
    test_saga_order_service_down()
    test_saga_inventory_service_down()
    test_saga_payment_service_down()
    test_saga_payment_service_slow()
    test_saga_payment_service_down_compensation_fails()
    test_saga_inventory_service_down_compensation_fails()
    test_saga_kafka_unreachable()
    test_saga_kafka_slow()
    test_saga_orchestrator_crashes()

def run_2pc_tests():
    test_baseline("2pc")
    test_2pc_order_db_hangs_prepare()
    test_2pc_inventory_db_hangs_prepare()
    test_2pc_payment_db_hangs_prepare()
    test_2pc_payment_db_hangs_commit_recovers()
    test_2pc_payment_db_tcp_reset_commit()
    test_2pc_slow_db_false_abort()
    test_2pc_inventory_db_hangs_commit_recovers()

def run_tcc_tests():
    test_baseline("tcc")
    test_tcc_order_hangs_try()
    test_tcc_inventory_hangs_try()
    test_tcc_payment_hangs_try()
    test_tcc_payment_hangs_confirm()
    test_tcc_inventory_hangs_cancel()
    test_tcc_slow_try_reservation_expiry()
    test_tcc_coordinator_crashes_during_try()
    test_tcc_coordinator_crashes_during_confirm()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Distributed transaction correctness tests")
    parser.add_argument("--protocol", choices=["saga", "2pc", "tcc", "all"], default="all")
    args = parser.parse_args()

    print("\n  Distributed Transaction Correctness Test Suite")
    print(f"  Protocol: {args.protocol.upper()}")
    print(f"  Checking Toxiproxy at {TOXIPROXY_HOST}...")

    try:
        requests.get(f"{TOXIPROXY_HOST}/proxies", timeout=3)
    except Exception:
        print(f"\n  ERROR: Cannot reach Toxiproxy at {TOXIPROXY_HOST}")
        print("  Make sure toxiproxy-server is running.\n")
        sys.exit(1)

    print("  Toxiproxy OK\n")

    if args.protocol in ("saga", "all"):
        run_saga_tests()
        print_summary("saga")
    if args.protocol in ("2pc", "all"):
        run_2pc_tests()
        print_summary("2pc")
    if args.protocol in ("tcc", "all"):
        run_tcc_tests()
        print_summary("tcc")

