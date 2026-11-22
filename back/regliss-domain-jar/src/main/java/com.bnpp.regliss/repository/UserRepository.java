package com.bnpp.regliss.repository;


import com.bnpp.regliss.entity.Profile;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.vo.UserBreadcrumbVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface UserRepository extends EntityRepository<User, Long>, UserRepositoryCustom, JpaRepository<User,Long> {

    User findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.username = ?1 AND u.status='ACTIVE'")
    User findActiveByUsername(String username);

    @Query("SELECT COUNT(u) FROM User u WHERE u.username = ?1")
    int countByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.assignedListPermissions WHERE u.username = ?1")
    User findByIdFetchListPermissions(String username);

    List<User> findByProfile(Profile profile);

    @Query("SELECT u FROM User u WHERE " +
                "(u.exportedFileDownloadPermissionEndDate <= :expirationDate AND u.exportedFileDownloadPermissionEndDate >= :ignoreDate) OR " +
                "(u.importedFileDownloadPermissionEndDate <= :expirationDate AND u.importedFileDownloadPermissionEndDate >= :ignoreDate)")
    List<User> findByDownloadPermissionEndDateExpiredBetween(@Param("expirationDate") LocalDate expirationDate, @Param("ignoreDate") LocalDate ignoreDate);

    @Query("SELECT user.id as id, user.username as username, user.firstName as firstName, user.lastName as lastName FROM User user WHERE user.id=?1")
    UserBreadcrumbVO getBreadcrumbForUser(long userId);


    @Query("SELECT uu, l.reference || '-' || l.shortName  "
            +" FROM User uu LEFT JOIN ReglissList l ON"
            +" (EXISTS (SELECT ulp"
            +" FROM UserListPermission ulp"
            +" WHERE ulp.user.id = uu.id AND l.id = ulp.reglissListId and l.deleted = false)"
            +" OR EXISTS (SELECT lpm "
            +" FROM ListPermissionMatrix lpm"
            +" WHERE l.confidentiality = lpm.confidentiality"
            +" AND l.deleted = false"
            +" AND (lpm.scope IS NULL OR l.scope = lpm.scope)"
            +" AND (lpm.sameCountry IS NULL "
            +" OR (l.country.id = uu.country.id AND lpm.sameCountry = 1)"
            +" OR (l.country.id != uu.country.id and lpm.sameCountry = 0))"
            +" AND lpm.profile = uu.profile and lpm.defaultPerm IS NOT NULL"
            +" ))")
    List<Object[]> getUserForUsersList();

    @Modifying
    @Query(value = "UPDATE USERS U SET U.STATUS = :status WHERE U.USERNAME IN :userNames", nativeQuery = true)
    void updateStatusByUserName(@Param("userNames") List<String> userNames, @Param("status") String status);

    @Query(value = "SELECT * FROM USERS U WHERE U.USERNAME IN (:userNames)", nativeQuery = true)
    List<User> findByUserNames(@Param("userNames") List<String> userNames);

    @Query(value = "SELECT * FROM USERS U WHERE U.USERNAME = :userName", nativeQuery = true)
    User findByUID(@Param("userName") String userName);

    @Query(value = "SELECT * FROM USERS WHERE UPPER (USERNAME) = 'SYS' ORDER BY ID ASC", nativeQuery = true)
    List<User> findSysUser();
}

