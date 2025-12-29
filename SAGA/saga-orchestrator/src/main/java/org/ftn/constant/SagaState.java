package org.ftn.constant;

public enum SagaState {
    STARTED,
    ORDER_CREATED,
    INVENTORY_RESERVED,
    PAYMENT_COMPLETED,
    COMPLETED,
    FAILED,
    COMPENSATION_FAILED
}
