package com.bnpp.regliss.repository;

import java.util.List;

import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.search.UserSearchCriteria;
import com.bnpp.regliss.vo.UserWithAccessibleListsVO;

public interface UserRepositoryCustom {

    PaginatedResults<UserWithAccessibleListsVO> searchAPageForUI(UserSearchCriteria criteria);

    User getOneByUsername(String username);

    List<UserWithAccessibleListsVO> searchForExport(UserSearchCriteria criteria);
}

