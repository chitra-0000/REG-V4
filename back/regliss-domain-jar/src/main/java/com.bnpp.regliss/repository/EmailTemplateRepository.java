package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.EmailTemplate;
import com.bnpp.regliss.entity.EmailType;
import com.bnpp.regliss.repository.base.EntityRepository;

public interface EmailTemplateRepository extends EntityRepository<EmailTemplate, Long>{

    EmailTemplate findByType(EmailType type);

}

