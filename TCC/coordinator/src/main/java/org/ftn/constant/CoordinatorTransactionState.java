package org.ftn.constant;

public enum CoordinatorTransactionState {
    STARTED,
    PREPARING,
    COMMITTING,
    COMMITTED,
    ABORTING,
    ABORTED,
    COMMIT_FAILURE,
    ROLLBACK_FAILURE
}
