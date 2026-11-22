package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.EmailLog;
import com.bnpp.regliss.entity.EmailType;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.List;
import java.util.Optional;

public interface EmailLogRepository extends EntityRepository<EmailLog, Long> {

    List<EmailLog> findByTypeAndParametersAndSendingStatus(EmailType type, String parameters, EmailLog.SendingStatus sendingStatus);

    Optional<EmailLog> findByTypeAndParameters(EmailType type, String parameters);

}

