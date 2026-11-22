package com.bnpp.regliss.service;

import java.time.LocalDateTime;
import java.util.List;

import com.bnpp.regliss.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.Log;
import com.bnpp.regliss.entity.LogDetail;
import com.bnpp.regliss.entity.ObjectType;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.repository.AuditRepository;
import com.bnpp.regliss.repository.LogRepository;
import com.bnpp.regliss.search.UserSearchCriteria;
import com.bnpp.regliss.utils.AuditUtil;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRED, timeout = 100)
public class UserAuditService {

    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private LogRepository logRepo;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private UserRepository userRepository;

    public void auditCreate(User user){
        auditRepo.save(fill(new Audit(AuditedAction.USER_CREATION), user));
        if (user.isActive()) {
            auditRepo.save(fill(new Audit(AuditedAction.USER_ACTIVATION), user));
        }
    }

    public void auditUpdate(User oldUser, User newUser) {
        auditActivation(oldUser, newUser);
        auditFieldsChanged(oldUser, newUser);
    }


    public void auditUserLogin(){
        auditRepo.save(fill(new Audit(AuditedAction.USER_CONNEXION), requestContext.getCurrentUserRef()));
    }

    private void auditActivation(User oldUser, User newUser) {
        if (newUser.isActive() == oldUser.isActive()) {
            return; // nothing to do
        }
        if (newUser.isActive()) {
            auditRepo.save(fill(new Audit(AuditedAction.USER_ACTIVATION), oldUser));
        }
        if (!newUser.isActive()) {
            auditRepo.save(fill(new Audit(AuditedAction.USER_DEACTIVATION), oldUser));
        }
    }

    private void auditFieldsChanged(User oldUser, User newUser) {
        List<Audit> audits = ReflectionComparator.compare(oldUser, newUser);
        for (Audit audit : audits) {
            if(audit.getAuditedAction() == null) {
                audit.setAuditedAction(AuditedAction.USER_MODIFICATION);
            }
            auditRepo.save(fill(audit, oldUser));
        }
    }

    private <T extends LogDetail> T fill(T audit, User user) {
        if (requestContext.getCurrentUser() != null && requestContext.getRequestTime() != null) {
            audit.setUser(requestContext.getCurrentUserRef());
            audit.setLogDate(requestContext.getRequestTime());
        } else {
            List<User> sysUsers = userRepository.findSysUser();
            if (!(sysUsers.isEmpty())) {
                audit.setUser(sysUsers.get(0));
                audit.setLogDate(LocalDateTime.now());
            }
        }

        audit.setObjectType(ObjectType.USER);
        audit.setReference(user.getUsername());
        audit.setObjectId(user.getId());
        return audit;
    }


    public void logSearch(UserSearchCriteria criteriaObject) {
        Log log = new Log(AuditedAction.USER_SEARCH);
        log.setLogDate(requestContext.getRequestTime());
        log.setUser(requestContext.getCurrentUserRef());
        log.setObjectType(ObjectType.USER);
        log.setNewValue(AuditUtil.criteriaToAuditString(criteriaObject));
        logRepo.save(log);
    }
}
