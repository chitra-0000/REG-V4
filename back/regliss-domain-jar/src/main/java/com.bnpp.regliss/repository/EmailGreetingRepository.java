package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.EmailGreeting;
import com.bnpp.regliss.entity.EmailLanguage;
import com.bnpp.regliss.repository.base.EntityRepository;

public interface EmailGreetingRepository extends EntityRepository<EmailGreeting, Long> {
    EmailGreeting findByLanguage(EmailLanguage language);
}

