package com.bnpp.regliss.service;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.VersionLock;
import com.bnpp.regliss.repository.VersionLockRepository;
import com.bnpp.regliss.repository.VersionRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Optionals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.time.LocalDateTime.now;
import static java.util.Collections.singleton;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
public class VersionLockService {

    private static final String VERSION_LOCKED = "Version locked";
    @Autowired
    private VersionLockRepository lockRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ReferenceService referenceService;

    @PersistenceContext
    private EntityManager entityManager; //NOSONAR injection of entity manager using specific annotation

    @Setter
    @Value("${lock.expiration.minutes}")
    private long expirationMinutes;

    @Setter
    @Value("${automatic_version.lock.expiration.minutes}")
    private long automaticVersionExpirationMinutes;

    public void lock(Version version) { performLock(version, expirationMinutes, requestContext.getCurrentUserRef()); }

    public void lock(long versionId) {
        Version version = versionRepository.getExactlyOne(versionId);
        performLock(version, expirationMinutes, requestContext.getCurrentUserRef());
    }

    public void lock(long versionId, User user) {
        Version version = versionRepository.getExactlyOne(versionId);
        performLock(version, expirationMinutes, user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lockSemiAutoVersionInTx(Long versionId) {
        log.info("Locking manual version {}", versionId);
        unlockLockedByAnyUser(versionId);
        Version version = versionRepository.getExactlyOne(versionId);
        performLock(version,automaticVersionExpirationMinutes , referenceService.getSysUser());
        log.debug(VERSION_LOCKED);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlockSemiAutoVersionInTx(Long versionId) {
        log.info("Unlocking version {}", versionId);
        unlockLockedByAnyUser(versionId);
        log.debug(VERSION_LOCKED);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lockAutomaticVersionInTx(Long versionId) {
        log.info("Locking automatic version {}", versionId);
        Version version = versionRepository.getExactlyOne(versionId);
        performLock(version, automaticVersionExpirationMinutes, requestContext.getCurrentUserRef());
        log.debug(VERSION_LOCKED);
    }


    private void performLock(Version version, long expirationMinutes, User user) {
        VersionLock lastActiveLock = getLastActiveLock(version.getId());
        if (lastActiveLock == null) {
            entityManager.lock(version, LockModeType.PESSIMISTIC_WRITE);
            VersionLock newLock = new VersionLock();
            newLock.setVersionId(version.getId());
            newLock.setUser(user);
            newLock.setAcquireDate(now());
            newLock.setReleaseDate(now().plusMinutes(expirationMinutes));
            lockRepository.save(newLock);
        } else if (isOwnedByCurrentUser(lastActiveLock)) {
            entityManager.lock(version, LockModeType.PESSIMISTIC_WRITE);
            lastActiveLock.extendReleaseDate(expirationMinutes);
        }
    }

    public void checkAndExtendLock(Version version) {
        entityManager.lock(version, LockModeType.PESSIMISTIC_WRITE);
        VersionLock lastActiveLock = getLastActiveLock(version.getId());
        if (lastActiveLock != null && isOwnedByCurrentUser(lastActiveLock)) {
            lastActiveLock.extendReleaseDate(expirationMinutes);
        } else {
            throw new ReglissException(ErrorCode.LOST_LOCK);
        }
    }

    public boolean unlock(Version version) {
        entityManager.lock(version, LockModeType.PESSIMISTIC_WRITE);
        VersionLock lastActiveLock = getLastActiveLock(version.getId());
        if (lastActiveLock != null && isOwnedByCurrentUser(lastActiveLock)) {
            lastActiveLock.setReleaseDate(now());
            lastActiveLock.setReleaseUser(requestContext.getCurrentUserRef());
            return true;
        }

        return false;
    }

    public boolean unlock(long versionId) {
        Version version = versionRepository.getExactlyOne(versionId);
        return unlock(version);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlockAutomaticVersionInTx(Long versionId) {
        Optional<Version> version = versionRepository.findById(versionId);
        Optionals.ifPresentOrElse(version, this::unlockLockedByAnyUser, () -> log.debug("Version {} was not found in the database", versionId));
    }

    public void unlockLockedByAnyUser(Version version) {
        entityManager.lock(version, LockModeType.PESSIMISTIC_WRITE);
        VersionLock lastActiveLock = getLastActiveLock(version.getId());
        if (lastActiveLock != null) {
            lastActiveLock.setReleaseDate(now());
            lastActiveLock.setReleaseUser(requestContext.getCurrentUserRef());
        }
    }

    public void unlockLockedByAnyUser(long versionId) {
        Version version = versionRepository.getExactlyOne(versionId);
        unlockLockedByAnyUser(version);
    }

    public boolean isLockedByAnotherUser(Long idVersion) {
        VersionLock lastActiveLock = getLastActiveLock(idVersion);
        boolean isLocked = false;
        if (lastActiveLock != null && !isOwnedByCurrentUser(lastActiveLock)) {
            isLocked = true;
        }
        return isLocked;
    }

    public Optional<User> getLockingUser(Long idVersion) {
        VersionLock lock = getLastActiveLocks(singleton(idVersion)).get(idVersion);
        if (lock != null) {
            return Optional.ofNullable(lock.getUser());
        }
        return Optional.empty();
    }

    private VersionLock getLastActiveLock(Long idVersion) {
        return getLastActiveLocks(singleton(idVersion)).get(idVersion);
    }

    public Map<Long, VersionLock> getLastActiveLocks(Set<Long> idVersion) {
        return lockRepository.findLastLock(idVersion).stream().filter(VersionLock::isActive)
                .collect(toMap(VersionLock::getVersionId, identity()));
    }

    private boolean isOwnedByCurrentUser(VersionLock lock) { return lock.isOwnedBy(requestContext.getUserId()); }
    private boolean isOwnedBySysUser(VersionLock lock) { return lock.isOwnedBy(referenceService.getSysUser().getId()); }

    public void checkAndExtendAutomaticImportLock(long versionId) {
        Version version = versionRepository.getExactlyOne(versionId);
        VersionLock lastActiveLock = getLastActiveLock(version.getId());
        if (lastActiveLock != null && isOwnedBySysUser(lastActiveLock)) {
            lastActiveLock.extendReleaseDate(automaticVersionExpirationMinutes);
        } else {
            throw new ReglissException(ErrorCode.LOST_LOCK);
        }
    }
}
