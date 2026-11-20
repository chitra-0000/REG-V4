package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.search.UserSearchCriteria;
import com.bnpp.regliss.utils.ReflectionUtils;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import com.bnpp.regliss.vo.UserWithAccessibleListsVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.*;

import static java.util.stream.Collectors.*;

/*This class is mostly sql. In this case trying to factor into constants all the repeated literals
 * that appear in queries will just complicate them further.
 * For this reason we mark sonar rule: <String literals should not be duplicated -> squid:S1192> as false positive in this
 * class. Testing was performed on this queries through com.bnpp.regliss.repository.UserRepositoryLocalIT
 */
@Slf4j
public class UserRepositoryImpl extends BaseRepositoryImpl<User> implements UserRepositoryCustom{

    private static final String USERNAME = "username";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String ENTITY = "entity";
    private static final String BUSINESS_LINE = "businessLine";
    private static final String AND = " AND ";

    @Autowired
    private UserRepository repository;

    @Autowired
    private ReglissListRepository listRepo;


    private static final String USER_DETAILS_JPQL =
                    "SELECT uu, l.reference || ' - ' || l.shortName"
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
                              +" ))"
                    +" WHERE uu.id IN (%s)";


    static final Map<String, Object> SORT_KEYS_MAPPING = new HashMap<>();
    @PostConstruct
    private void setup() {
        SORT_KEYS_MAPPING.put(USERNAME, "UPPER(u.username)");
        SORT_KEYS_MAPPING.put(FIRST_NAME, "UPPER(u.firstName)");
        SORT_KEYS_MAPPING.put(LAST_NAME, "UPPER(u.lastName)");
        SORT_KEYS_MAPPING.put("country", "u.country.label");
        SORT_KEYS_MAPPING.put("pole", "UPPER(u.pole)");
        SORT_KEYS_MAPPING.put("profile", "UPPER(u.profile)");
        SORT_KEYS_MAPPING.put(ENTITY, "UPPER(u.entity)");
        SORT_KEYS_MAPPING.put(BUSINESS_LINE, "UPPER(u.businessLine)");
        SORT_KEYS_MAPPING.put("downloadExportRight", "(CASE WHEN NVL(u.exportedFileDownloadPermissionEndDate, CURRENT_TIMESTAMP) > CURRENT_TIMESTAMP THEN 1 ELSE 0 END)");
        SORT_KEYS_MAPPING.put("downloadImportRight", "(CASE WHEN NVL(u.importedFileDownloadPermissionEndDate, CURRENT_TIMESTAMP) > CURRENT_TIMESTAMP THEN 1 ELSE 0 END)");
        SORT_KEYS_MAPPING.put("status", "u.status");
        SORT_KEYS_MAPPING.put(PaginationService.DEFAULT_ORDER_KEY,
                Sort.by(new Order(Direction.ASC, "u.status"))
                    .and(Sort.by(new Order(Direction.ASC, "UPPER(u.lastName)")))
                    .and(Sort.by(new Order(Direction.ASC, "UPPER(u.firstName)")))
                    );
    }

    @Autowired
    private PaginationService paginationService;

    @Autowired
    private EntityManager em;

    /unchecked/
    @Override
    public PaginatedResults<UserWithAccessibleListsVO> searchAPageForUI(UserSearchCriteria criteria) {
        ReflectionUtils.trimAllStringFields(criteria);
        Map<String, Object> params = new HashMap<>();

        String selectIdsJpql = getJpqlForSearchCriteria(criteria, params);

        PageRequest pageRequest = paginationService.toPageRequestWithAtMostOneDefaultOrder(criteria.paginationOptions, SORT_KEYS_MAPPING);
        List<Long> userIdsOrderedPage = paginationService.createPaginatedQuery("SELECT u.id " + selectIdsJpql, params, pageRequest).getResultList();
        long totalCount = paginationService.countTotal("SELECT COUNT(*) " + selectIdsJpql, params);

        log.debug("A page of user ids: " + userIdsOrderedPage);

        if (userIdsOrderedPage.isEmpty()) {
            return PaginatedResults.empty();
        }

        return getUsersDetails(userIdsOrderedPage, totalCount);
    }

    /unchecked/
    private PaginatedResults<UserWithAccessibleListsVO> getUsersDetails(List<Long> userIdsOrderedPage, long totalCount) {
        String jpql = String.format(USER_DETAILS_JPQL, ":pageRecordIds");

//      LambdaUtil.partition(users.stream(), 1000)

        Query query = entityManager.createQuery(jpql); // NOSONAR
        query.setParameter("pageRecordIds", userIdsOrderedPage);

        List<Object[]> results = query.getResultList();
        Map<User, List<String>> userToLists = results.stream()
                .collect(groupingBy(result -> (User) result[0],
                                    mapping(result -> (String) result[1], toList())));
        Map<Long, UserWithAccessibleListsVO> vos = userToLists.entrySet().stream()
                .map(entry -> new UserWithAccessibleListsVO(entry.getKey(), entry.getValue()))
                .collect(toMap(vo -> vo.getUser().getId(), vo -> vo));
        return new PaginatedResults<>(userIdsOrderedPage.stream().map(vos::get).collect(toList()), totalCount);
    }

    /unchecked/
    @Override
    public List<UserWithAccessibleListsVO> searchForExport(UserSearchCriteria criteria) {
        ReflectionUtils.trimAllStringFields(criteria);

        criteria.paginationOptions.pageSize = Integer.MAX_VALUE;
        criteria.paginationOptions.pageNumber = 1;

        List<User> users = getUsers(criteria);

        Map<Long, UserWithAccessibleListsVO> usersWithLists = users.stream()
                .map(user -> new UserWithAccessibleListsVO(user, new ArrayList<>()))
                .collect(toMap(vo -> vo.getUser().getId(),
                        vo -> vo, (a,b)->a,
                        LinkedHashMap::new));

        log.debug("User count : " + users.size());

        Map<Long, String> listDisplayNames = listRepo.getAllListExportNames().stream()
                .collect(toMap(arr -> (Long)arr[0], arr -> (String)arr[1]));

        List<Pair<Long, Long>> userDefaultListIds = getUserDefaultListIds(criteria);
        userDefaultListIds.stream()
            .map(userIdToListId -> new Pair<>(userIdToListId.a, listDisplayNames.get(userIdToListId.b)))
            .forEach(pair -> usersWithLists.get(pair.getA()).getListNames().add(pair.getB()));

        List<Pair<Long, Long>> userManualListIds = getUserManualListIds(criteria);
        userManualListIds.stream()
            .map(userIdToListId -> new Pair<>(userIdToListId.a, listDisplayNames.get(userIdToListId.b)))
            .forEach(pair -> usersWithLists.get(pair.getA()).getListNames().add(pair.getB()));

        log.debug("Finished");

        return new ArrayList<>(usersWithLists.values());
    }

    /findsecbugs:SQL_INJECTION_JPA/
    /**
     * findsecbugs:SQL_INJECTION_JPA - The input values included in SQL queries need to be passed in safely.
     * Bind variables in prepared statements can be used to easily mitigate the risk of SQL injection
     * The parameters are set using the binding mechanism see PaginationService.setParamsOnQuery(params, queryForManual)
     * The sql query is complex and created via string concatenation leading to a false positive on sonar rule.
     */
    private List<User> getUsers(UserSearchCriteria criteria) {
        Map<String, Object> params = new HashMap<>();
        String selectIdsJpql = getJpqlForSearchCriteria(criteria, params);
        PageRequest pageRequest = paginationService.toPageRequestWithAtMostOneDefaultOrder(criteria.paginationOptions, SORT_KEYS_MAPPING);
        return paginationService.createPaginatedQuery("SELECT u " + selectIdsJpql, params, pageRequest).getResultList();
    }

    /findsecbugs:SQL_INJECTION_JPA/
    /**
     * findsecbugs:SQL_INJECTION_JPA - The input values included in SQL queries need to be passed in safely.
     * Bind variables in prepared statements can be used to easily mitigate the risk of SQL injection
     * The parameters are set using the binding mechanism see PaginationService.setParamsOnQuery(params, queryForManual)
     * The sql query is complex and created via string concatenation leading to a false positive on sonar rule.
     */
    private List<Pair<Long, Long>> getUserManualListIds(UserSearchCriteria criteria) {
        Map<String, Object> params = new HashMap<>();
        String selectIdsJpql = getJpqlForSearchCriteria(criteria, params);

        TypedQuery<Object[]> queryForManual = em.createQuery("SELECT ulp.user.id, l.id "
                +" FROM UserListPermission ulp "
                + " INNER JOIN ReglissList l ON l.id = ulp.reglissListId "
                +" WHERE ulp.user.id IN (SELECT u.id " + selectIdsJpql + ") ", Object[].class);
        PaginationService.setParamsOnQuery(params, queryForManual);
        return queryForManual.getResultList().stream()
            .map(arr -> new Pair<>((Long)arr[0], (Long)arr[1]))
            .collect(toList());
    }

    /findsecbugs:SQL_INJECTION_JPA/
    /**
     * findsecbugs:SQL_INJECTION_JPA - The input values included in SQL queries need to be passed in safely.
     * Bind variables in prepared statements can be used to easily mitigate the risk of SQL injection
     * The parameters are set using the binding mechanism see PaginationService.setParamsOnQuery(params, queryForDefault)
     * The sql query is complex and created via string concatenation leading to a false positive on sonar rule.
     */
    private List<Pair<Long, Long>> getUserDefaultListIds(UserSearchCriteria criteria) {
        Map<String, Object> params = new HashMap<>();
        String selectIdsJpql = getJpqlForSearchCriteria(criteria, params);

        TypedQuery<Object[]> queryForDefault = em.createQuery("SELECT distinct u.id, l.id "
            +" FROM User u "
            + " INNER JOIN ListPermissionMatrix lpm ON lpm.profile = u.profile "
            + " INNER JOIN ReglissList l ON l.confidentiality = lpm.confidentiality "
            + " WHERE l.deleted = false"
            +" AND u.id IN (SELECT u.id " + selectIdsJpql + ")"
            +" AND (lpm.scope IS NULL OR l.scope = lpm.scope)"
            +" AND (lpm.sameCountry IS NULL "
            +" OR (l.country.id = u.country.id AND lpm.sameCountry = 1)"
            +" OR (l.country.id != u.country.id and lpm.sameCountry = 0))"
            +" AND lpm.defaultPerm IS NOT NULL", Object[].class);
        PaginationService.setParamsOnQuery(params, queryForDefault);
        return queryForDefault.getResultList().stream()
            .map(arr -> new Pair<>((Long)arr[0], (Long)arr[1]))
            .collect(toList());
    }

    @Data
    public static class Pair<A,B> {
        private final A a;
        private final B b;
    }

    private String getJpqlForSearchCriteria(UserSearchCriteria criteria, Map<String, Object> params) {
        String selectIdsJpql = "FROM User u "
                + "WHERE u.username != 'sys' ";

        if (StringUtils.isNotBlank(criteria.username)) {
            selectIdsJpql += AND + TextNormalizerQueryUtil.createStringComparisonJpql("u.usernameNormalized", USERNAME, criteria.username) + " ";
            params.put(USERNAME, TextNormalizerQueryUtil.normalize(criteria.username));
        }

        if (StringUtils.isNotBlank(criteria.firstName)) {
            selectIdsJpql += AND + TextNormalizerQueryUtil.createStringComparisonJpql("u.firstNameNormalized", FIRST_NAME, criteria.firstName) + " ";
            params.put(FIRST_NAME, TextNormalizerQueryUtil.normalize(criteria.firstName));
        }

        if (StringUtils.isNotBlank(criteria.lastName)) {
            selectIdsJpql += AND + TextNormalizerQueryUtil.createStringComparisonJpql("u.lastNameNormalized", LAST_NAME, criteria.lastName) + " ";
            params.put(LAST_NAME, TextNormalizerQueryUtil.normalize(criteria.lastName));
        }

        if (criteria.countryId != null) {
            selectIdsJpql += " AND u.country.id = :countryId ";
            params.put("countryId", criteria.countryId);
        }

        if (StringUtils.isNotBlank(criteria.pole)) {
            selectIdsJpql += AND + TextNormalizerQueryUtil.createStringComparisonJpql("u.poleNormalized", "pole", criteria.pole) + " ";
            params.put("pole", TextNormalizerQueryUtil.normalize(criteria.pole));
        }

        if (StringUtils.isNotBlank(criteria.entity)) {
            selectIdsJpql += AND + TextNormalizerQueryUtil.createStringComparisonJpql("u.entityNormalized", ENTITY, criteria.entity) + " ";
            params.put(ENTITY, TextNormalizerQueryUtil.normalize(criteria.entity));
        }

        if (StringUtils.isNotBlank(criteria.businessLine)) {
            selectIdsJpql += AND + TextNormalizerQueryUtil.createStringComparisonJpql("u.businessLineNormalized", BUSINESS_LINE, criteria.businessLine) + " ";
            params.put(BUSINESS_LINE, TextNormalizerQueryUtil.normalize(criteria.businessLine));
        }

        if (StringUtils.isNotBlank(criteria.listName)){
            String formattedListName = TextNormalizerQueryUtil.createStringComparisonJpql("lst.shortNameNormalized", "listName", criteria.listName);
            selectIdsJpql +=" AND ( EXISTS (SELECT lst "
                  +" FROM UserListPermission ulp "
                  +" JOIN ReglissList lst ON lst.id = ulp.reglissListId"
                  +" WHERE ulp.user.id = u.id"
                  + AND +formattedListName
              +" )"
              +" OR EXISTS (SELECT lst "
              +" FROM ListPermissionMatrix lpm"
              +" JOIN ReglissList lst ON lst.confidentiality = lpm.confidentiality"
                    +" AND (lpm.scope IS NULL OR lst.scope = lpm.scope)"
                    +" AND (lpm.sameCountry IS NULL"
                        +" OR (lst.country.id = u.country.id AND lpm.sameCountry = 1)"
                        +" OR (lst.country.id != u.country.id AND lpm.sameCountry = 0))"
              +" WHERE lpm.profile = u.profile AND lpm.defaultPerm IS NOT NULL"
                + AND +formattedListName
              +") )";
            params.put("listName", TextNormalizerQueryUtil.normalize(criteria.listName));
        }

        return selectIdsJpql;
    }

    @Override
    public User getOneByUsername(String username) {
        User user = repository.findActiveByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Username not found in DB: " + username);
        }
        return user;
    }

}

