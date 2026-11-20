package com.bnpp.regliss.lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.ExpirableLockRegistry;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
@Service
@Primary
public class DistributedLockManager implements DistributedLocks {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockManager.class);
    private final long lockTimeoutMillis;
    private final ExpirableLockRegistry lockRegistry;
    private final LockRepository lockRepository;

    public DistributedLockManager(ExpirableLockRegistry lockRegistry,
                                  LockRepository lockRepository,
                                  @Value("${distributed.lock.minutes.timeout:180}") Long minutesToLockTimeout) {
        this.lockRegistry = Objects.requireNonNull(lockRegistry);
        this.lockTimeoutMillis = TimeUnit.MINUTES.toMillis(minutesToLockTimeout);
        this.lockRepository = lockRepository;
        LOGGER.info("distributed_lock_timeout={} minutes", minutesToLockTimeout);
    }

    @Override
    public boolean isLockAcquired(DistributedLock lock) { return lockRepository.isAcquired(lock.getLockName()); }

    @Override
    public void executeWithDistributedLock(DistributedLockHandler handler) {
        LOGGER.debug("Trying to acquire lock {}", handler.getLock());

        //Expire old locks that exceeded lock time to live
        lockRegistry.expireUnusedOlderThan(lockTimeoutMillis);

        Lock lock = lockRegistry.obtain(handler.getLock().getLockName());

        if (lock.tryLock()){
            LOGGER.debug("Lock {} acquired", handler.getLock());
            try {
                handler
                        .getOnSuccess()
                        .accept(handler.getLock());
                LOGGER.debug("Executed <<success>> operation. Releasing lock {}", handler.getLock());
            }catch (RuntimeException ex){
                LOGGER.error("Exception caught while executing onSuccess()", ex);
            }finally {
                lock.unlock();
            }
        }else{
            LOGGER.debug("Couldn't acquire lock {}", handler.getLock());
            handler.getOnFailure().accept(handler.getLock());
            LOGGER.debug("Executed <<failure>> operation.");
        }
    }

    @Override
    public void releaseLock(DistributedLock distributedLock) {
        Lock lock = lockRegistry.obtain(distributedLock.getLockName());
        lock.unlock();
    }
}

