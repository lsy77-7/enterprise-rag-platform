package com.example.rag.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfterCommitExecutorTest {

    private final AfterCommitExecutor executor = new AfterCommitExecutor();

    @Test
    void shouldRunAfterTransactionCommit() {
        AtomicBoolean executed = new AtomicBoolean(false);
        TransactionSynchronizationManager.initSynchronization();

        try {
            executor.executeAfterCommit(() -> executed.set(true));
            assertFalse(executed.get());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            assertTrue(executed.get());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldRunImmediatelyWhenNoTransactionIsActive() {
        AtomicBoolean executed = new AtomicBoolean(false);

        executor.executeAfterCommit(() -> executed.set(true));

        assertTrue(executed.get());
    }
}
