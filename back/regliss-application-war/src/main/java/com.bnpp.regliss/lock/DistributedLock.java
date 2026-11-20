package com.bnpp.regliss.lock;

import javax.validation.constraints.NotEmpty;

public class DistributedLock {
    @NotEmpty
    private String lockName;

    public DistributedLock(String lockName) { this.lockName = lockName; }

    public String getLockName() { return this.lockName; }
}

