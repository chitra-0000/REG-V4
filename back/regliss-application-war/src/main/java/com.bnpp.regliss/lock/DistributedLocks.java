package com.bnpp.regliss.lock;

public interface DistributedLocks {
    boolean isLockAcquired(DistributedLock var1);

    void executeWithDistributedLock(DistributedLockHandler var1);

    void releaseLock(DistributedLock var1);
}

