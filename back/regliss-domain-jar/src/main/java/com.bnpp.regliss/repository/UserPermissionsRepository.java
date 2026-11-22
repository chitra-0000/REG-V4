package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.bnpp.regliss.entity.UserListPermission;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.List;

public interface UserPermissionsRepository extends EntityRepository<UserListPermission, Long> {

    @Modifying
    @Query("DELETE from UserListPermission where reglissListId = ?1")
    void deleteByReglissList(Long list);

    List<UserListPermission> findByUserAndReglissListId(User user, Long listId);

}

