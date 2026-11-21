package com.bnpp.regliss.web.security;

import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserTransactionUtil {

    @Autowired
    private UserRepository userRepository;

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void persistAndCommitUser(User user) {
        userRepository.save(user);
    }

}

