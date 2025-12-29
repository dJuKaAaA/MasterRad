package org.ftn.utils;

@FunctionalInterface
public interface RollbackAction {
    void rollback();
}
