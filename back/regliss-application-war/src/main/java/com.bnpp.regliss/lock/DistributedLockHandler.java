package com.bnpp.regliss.lock;

import java.util.function.Consumer;

public class DistributedLockHandler {
    private DistributedLock distributedLock;
    private Consumer<DistributedLock> onSuccess;
    private Consumer<DistributedLock> onFailure;

    private DistributedLockHandler() {
    }

    public DistributedLock getLock() { return this.distributedLock; }

    public Consumer<DistributedLock> getOnSuccess() { return this.onSuccess; }

    public Consumer<DistributedLock> getOnFailure() { return this.onFailure; }

    public static final class Builder {
        private DistributedLock distributedLock;
        private Consumer<DistributedLock> onSuccess = (lock) -> {
        };
        private Consumer<DistributedLock> onFailure = (lock) -> {
        };

        public Builder() {
         //nothing to implement
        }

        public DistributedLockHandler.Builder lock(DistributedLock distributedLock) {
            this.distributedLock = distributedLock;
            return this;
        }

        public DistributedLockHandler.Builder onSuccess(Consumer<DistributedLock> onSuccess) {
            this.onSuccess = onSuccess;
            return this;
        }

        public DistributedLockHandler.Builder onFailure(Consumer<DistributedLock> onFailure) {
            this.onFailure = onFailure;
            return this;
        }

        public DistributedLockHandler build() {
            DistributedLockHandler distributedLockHandler = new DistributedLockHandler();
            distributedLockHandler.onFailure = this.onFailure;
            distributedLockHandler.distributedLock = this.distributedLock;
            distributedLockHandler.onSuccess = this.onSuccess;
            return distributedLockHandler;
        }
    }
}


