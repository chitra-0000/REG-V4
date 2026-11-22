package com.bnpp.regliss.service;

import com.bnpp.regliss.entity.Language;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.repository.UserRepository;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Locale;

public class ReglissRequestContext {
    @Autowired
    private UserRepository userRepository;

    private String requestId;
    private LocalDateTime requestTime = LocalDateTime.now();
    private boolean highAuthentication;

    /** WARNING: THIS ENTITY IS DETACHED !! NOT BOUND TO ANY TRANSACTION. */
    private User currentUser;

    /** WARNING: THIS ENTITY IS DETACHED !! NOT BOUND TO ANY TRANSACTION. */
    public User getCurrentUser() { return currentUser; }

    public User getCurrentUserRef() { return userRepository.getReference(currentUser.getId()); }

    public User getCurrentUserEager() { return userRepository.getExactlyOne(currentUser.getId()); }

    public String getInstanceIdForDebug() {
        return System.identityHashCode(this)+" on thread "  + Thread.currentThread().getName() + " / " + Thread.currentThread().getId();
    }

    public void restoreFrom(ReglissRequestContext source) {
        this.currentUser = source.currentUser;
        this.requestId = source.requestId;
        this.requestTime = source.requestTime;
        this.highAuthentication = source.highAuthentication;
    }

    public Long getUserId() { return currentUser.getId(); }

    public String getRequestId() { return requestId; }

    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getUsername() { return currentUser.getUsername(); }

    public Language getLanguage() { return currentUser.getLanguage(); }

    public LocalDateTime getRequestTime() { return requestTime; }

    public boolean isHighAuthentication() { return highAuthentication; }

    public boolean isLowAuthentication() { return !highAuthentication; }

    public void setHighAuthentication(boolean highAuthentication) { this.highAuthentication = highAuthentication; }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString( /* object: */ this) + " instance="+System.identityHashCode( /* this: */ this)+" on thread " + Thread.currentThread().getName();
    }

    public Locale getLocale() {
        if (getLanguage() != null) {
            return new Locale(getLanguage().name());
        } else {
            return Locale.ENGLISH;
        }
    }

    public void setRequestTime(LocalDateTime requestTime) { this.requestTime = requestTime; }

    public void setCurrentUser(User currentUser) { this.currentUser = currentUser; }
}
