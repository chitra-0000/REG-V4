package com.bnpp.regliss.facade;

import static com.bnpp.regliss.entity.User.Status.ACTIVE;
import static java.util.stream.Collectors.*;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.*;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.facade.dto.administration.UserBreadcrumbDto;
import com.bnpp.regliss.facade.dto.administration.UserDetailsDto;
import com.bnpp.regliss.facade.dto.administration.UserForUsersListDto;
import com.bnpp.regliss.facade.dto.list.*;
import com.bnpp.regliss.repository.ListPermissionMatrixRepository;
import com.bnpp.regliss.vo.UserBreadcrumbVO;
import org.springframework.beans.factory.annotation.Autowired;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.client.refog.RefogClient;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.exporter.users.UserSearchExportService;
import com.bnpp.regliss.facade.dto.LoggedInUserDto;
import com.bnpp.regliss.facade.dto.administration.UserDto;
import com.bnpp.regliss.facade.mapper.UserMapper;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.CountryRepository;
import com.bnpp.regliss.repository.UserRepository;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.search.UserSearchCriteria;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.service.UserAuditService;
import com.bnpp.regliss.service.permission.CurrentFeaturePermissions;
import com.bnpp.regliss.service.permission.CurrentListPermissions;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.vo.UserWithAccessibleListsVO;

@Facade
@ReglissWebProfile
public class UserFacade {

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private UserRepository repository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RefogClient refogClient;

    @Autowired
    private UserAuditService auditService;

    @Autowired
    private UserSearchExportService searchExportService;

    @Autowired
    private CurrentListPermissions currentListPermissions;

    @Autowired
    private CurrentFeaturePermissions currentFeaturePermissions;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ListPermissionMatrixRepository listPermissionMatrixRepository;

    public LoggedInUserDto getLoggedInUser() {
        User user = requestContext.getCurrentUser();
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.id = user.getId();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.username = user.getUsername();
        dto.email = user.getEmail();
        dto.language = user.getLanguage().name();
        dto.fullName = user.getFullName();
        dto.profile = user.getProfile();
        Set<FeaturePermission> permissionSet = currentFeaturePermissions.getFeaturePermissions();
        for (FeaturePermission feature : FeaturePermission.values()) {
            dto.permissions.put(feature, permissionSet.contains(feature));
        }
        return dto;
    }

    public void updateLastConnectionDate() {
        User user = requestContext.getCurrentUser();
        User userInDb = this.repository.getOneByUsername(user.getUsername());
        User newUser = new User(userInDb);
        newUser.setLastConnection(LocalDateTime.now());
        repository.save(newUser);
        auditService.auditUpdate(userInDb, newUser);

    }

    public PaginatedResults<UserDto> search(UserSearchCriteria criteria) {
        auditService.logSearch(criteria);
        PaginatedResults<UserWithAccessibleListsVO> paginatedEntities = repository.searchAPageForUI(criteria);
        return paginatedEntities.map(userMapper::toSearchResultDto);
    }

    public List<UserForUsersListDto> getUserForUsersList() {
        checkUserForUserListPermission(requestContext.getUserId());
        List<Object[]> results = repository.getUserForUsersList();
        Map<User, List<String>> userToLists = results.stream()
                .collect(groupingBy(result -> (User) result[0],
                        mapping(result -> (String) result[1], toList())));
        List<UserWithAccessibleListsVO> vos = userToLists.entrySet().stream()
                .map(entry -> new UserWithAccessibleListsVO(entry.getKey(), entry.getValue()))
                .collect(toList());

        return vos.stream().map(vo -> userMapper.toUserForUsersListDto(vo)).collect(toList());
    }

    public void checkUserForUserListPermission(Long userId){
        User user = repository.getExactlyOne(userId);
        Profile profile = user.getProfile();
        Set<FeaturePermission> featurePermission = profile.getBaseFeatures();
        if(!featurePermission.contains(FeaturePermission.searchUsers)) {
            throw new ReglissException(ErrorCode.NOT_ALLOWED_BY_PROFILE);
        }
    }

    public Long create(UserDto dto) {
        if (repository.countByUsername(dto.username) > 0) {
            throw new ReglissException(ErrorCode.CREATE_USER_UID_ALREADY_USED);
        }

        checkChangeExportDownloadPermission(dto.hasExportDownloadPermission, dto.profile, dto.hasExportDownloadPermission);
        checkChangeImportDownloadPermission(dto.hasImportDownloadPermission, dto.profile, dto.hasImportDownloadPermission);

        User user = userMapper.fromDtoForCreate(dto);
        repository.save(user);
        auditService.auditCreate(user);
        return user.getId();
    }

    private void checkChangeExportDownloadPermission(boolean changedPermission, Profile profile, boolean hasExportDownloadPermission) {
        if (!changedPermission) {
            return;
        }
        if (hasExportDownloadPermission &&
                !profile.getBaseFeatures().contains(FeaturePermission.userDownloadExportVersionPermissionAssignable)) {
            throw new IllegalArgumentException("Not assignable");
        }
        Map<FeaturePermission, Boolean> permissions = getLoggedInUser().permissions;
        if (permissions != null && !permissions.get(FeaturePermission.userDownloadExportVersionPermissionModify)) {
            throw new IllegalArgumentException("Current user not allowed to assign");
        }
    }

    private void checkChangeImportDownloadPermission(boolean changedPermission, Profile profile, boolean hasImportDownloadPermission) {
        if (!changedPermission) {
            return;
        }
        if (hasImportDownloadPermission &&
                !profile.getBaseFeatures().contains(FeaturePermission.userDownloadImportVersionPermissionAssignable)) {
            throw new IllegalArgumentException("Not assignable");
        }
        Map<FeaturePermission, Boolean> permissions = getLoggedInUser().permissions;
        if (permissions != null && !permissions.get(FeaturePermission.userDownloadImportVersionPermissionModify)) {
            throw new IllegalArgumentException("Current user not allowed to assign");
        }
    }


    public void update(UserDto dto) {

        String currentLoggedInUsername = requestContext.getUsername();
        User userInDb = repository.getExactlyOne(dto.id);


        checkChangeExportDownloadPermission(dto.hasExportDownloadPermission != userInDb.hasExportedFileDownloadPermissionNow(),
                dto.profile, dto.hasExportDownloadPermission);
        checkChangeImportDownloadPermission(dto.hasImportDownloadPermission != userInDb.hasImportedFileDownloadPermissionNow(),
                dto.profile, dto.hasImportDownloadPermission);

        User oldUser = new User(userInDb);
        User newUser = userMapper.toEntity(userInDb, dto);
        if (newUser.getUsername().equals(currentLoggedInUsername)) {
            throw new ReglissException(ErrorCode.CANNOT_EDIT_PROFILE);
        }
        repository.save(newUser);
        auditService.auditUpdate(oldUser, newUser);
    }

    public void updateDetails(UserDetailsDto dto) {
        String currentLoggedInUsername = requestContext.getUsername();
        User userInDb = repository.getExactlyOne(dto.id);

        checkChangeExportDownloadPermission(dto.hasExportDownloadPermission != userInDb.hasExportedFileDownloadPermissionNow(),
                dto.profile, dto.hasExportDownloadPermission);
        checkChangeImportDownloadPermission(dto.hasImportDownloadPermission != userInDb.hasImportedFileDownloadPermissionNow(),
                dto.profile, dto.hasImportDownloadPermission);

        User oldUser = new User(userInDb);
        User newUser = userMapper.toEntity(userInDb, dto);
        if (newUser.getUsername().equals(currentLoggedInUsername)) {
            throw new ReglissException(ErrorCode.CANNOT_EDIT_PROFILE);
        }
        repository.save(newUser);
        auditService.auditUpdate(oldUser, newUser);
    }

    public void updatePermissionsAndAssignedLists(Long userId, Long countryId, Profile profile,
                                                  ReglissListAllPermissionsAndAssignedListsDto dto) {
        String currentLoggedInUsername = requestContext.getUsername();
        User userInDb = repository.getExactlyOne(userId);
        Country country = countryRepository.getExactlyOne(countryId);

        User oldUser = new User(userInDb);
        User newUser = userMapper.toUserEntity(userInDb, dto, country, profile);
        if (newUser.getUsername().equals(currentLoggedInUsername)) {
            throw new ReglissException(ErrorCode.CANNOT_EDIT_PROFILE);
        }
        repository.save(newUser);
        auditService.auditUpdate(oldUser, newUser);
    }

    public void removeDownloadPermissions(Long userId, ListPermission downloadType){
        String currentLoggedInUsername = requestContext.getUsername();
        User user = repository.getExactlyOne(userId);

        User oldUser = new User(user);

        Set<UserListPermission> newPerm;
        switch(downloadType){
            case DOWNLOADIMPORT:
                newPerm = user.getAssignedListPermissions().stream().filter(perm -> perm.getPermission() != ListPermission.DOWNLOADIMPORT)
                        .collect(toSet());
                user.setAssignedListPermissions(newPerm);
                break;
// [MISSING FROM SCREENSHOTS: lines 239-242 of the original file were not visible in the photos]
                break;
            default:
                return;
        }
        if (user.getUsername().equals(currentLoggedInUsername)) {
            throw new ReglissException(ErrorCode.CANNOT_EDIT_PROFILE);
        }
        repository.save(user);
        auditService.auditUpdate(oldUser, user);
    }

    public UserDto searchRefogForCreate(String username) {
        if (repository.countByUsername(username) != 0) {
            throw new ReglissException(ErrorCode.CREATE_USER_UID_ALREADY_USED);
        }
        User user = refogClient.getExactlyOne(username);
        user.setStatus(ACTIVE);
        return userMapper.toDto(user);
    }

    public void saveLanguage(Language language) {
        User user = repository.getExactlyOne(requestContext.getUserId());
        user.setLanguage(language);
    }


    public ReglissListAllPermissionsDto getListPermissionsForUser(Profile profile, Long countryId) {
        ReglissListAllPermissionsDto reglissListAllPermissionsDto = new ReglissListAllPermissionsDto();
        Country country = countryRepository.getExactlyOne(countryId);

        reglissListAllPermissionsDto.allGrantedLists =
                currentListPermissions.getAssignableListPermissions(profile, country).stream()
                        .map(userMapper::toDto)
                        .collect(toList());

        reglissListAllPermissionsDto.defaultGrantedLists =
                currentListPermissions.getDefaultListPermissions(profile, country).stream()
                        .map(userMapper::toDto)
                        .collect(toList());
        return reglissListAllPermissionsDto;
    }

    public ReglissListAllPermissionsAndAssignedListsDto getListPermissionsAndAssignedListsForUser(Long userId, Profile profile, Long countryId) {
        ReglissListAllPermissionsAndAssignedListsDto reglissListAllPermissionsAndAssignedListsDto =
                new ReglissListAllPermissionsAndAssignedListsDto();
        User user = repository.getExactlyOne(userId);
        Country country = countryRepository.getExactlyOne(countryId);

        List<ReglissListPermissionDetailsDto> allGrantedListsWithoutAdditionalRights = userMapper
// [MISSING FROM SCREENSHOTS: lines 292-293 of the original file were not visible in the photos]

        reglissListAllPermissionsAndAssignedListsDto.allGrantedLists = userMapper
                .getAllGrantedListsWithAdditionalRights(allGrantedListsWithoutAdditionalRights, additionalRightsForAllGrantedLists);

        List<ReglissListPermissionDetailsDto> defaultGrantedListsWithoutAdditionalRights = userMapper
                .getDefaultGrantedListsWithoutAdditionalRights(country, profile);
        List<ReglissListPermissionDetailsDto> additionalRightsForDefaultGrantedLists = userMapper
                .getDefaultListsAdditionalRights(user, profile, country);

        reglissListAllPermissionsAndAssignedListsDto.defaultGrantedLists = userMapper
                .getDefaultGrantedListsWithAdditionalRights(defaultGrantedListsWithoutAdditionalRights, additionalRightsForDefaultGrantedLists);

        this.disableWritePermissionsForDefaultLists(reglissListAllPermissionsAndAssignedListsDto.defaultGrantedLists,
                allGrantedListsWithoutAdditionalRights);

        reglissListAllPermissionsAndAssignedListsDto.assignedLists = userMapper.mapAssignedLists(userMapper.getAdditionalLists(user,
                reglissListAllPermissionsAndAssignedListsDto.defaultGrantedLists));
        this.disableWritePermissionsForAssignedLists(reglissListAllPermissionsAndAssignedListsDto.assignedLists,
                allGrantedListsWithoutAdditionalRights);

        return reglissListAllPermissionsAndAssignedListsDto;
    }

// [MISSING FROM SCREENSHOTS: lines 317-319 of the original file were not visible in the photos]
            boolean containsList = allGrantedListsWithoutAdditionalRights.stream().anyMatch(l -> l.listId.equals(dl.listId));
            if(!containsList){
                dl.setWritePermissionDisabled(true);
            }
        }
    }

    private void disableWritePermissionsForAssignedLists(List<ReglissListPermissionDetailsDto> assignedLists,
                                                         List<ReglissListPermissionDetailsDto> allGrantedListsWithoutAdditionalRights){
        for(ReglissListPermissionDetailsDto al: assignedLists){
            for(ReglissListPermissionDetailsDto agl: allGrantedListsWithoutAdditionalRights){
                if(al.listId.equals(agl.listId) && !al.writePermission && !agl.writePermission) al.setWritePermissionDisabled(true);
            }
        }
    }

    public AllGrantedListsDto getAllGrantedLists(long userId, Profile profile, Long countryId){
        AllGrantedListsDto allGrantedListsDto = new AllGrantedListsDto();
        User user = repository.getExactlyOne(userId);
        Country country = countryRepository.getExactlyOne(countryId);

        Set<UserListPermission> assignedListPermissions = user.getAssignedListPermissions();
        List<ReglissListPermissionDetailsDto> allGrantedListsWithoutAdditionalRights = userMapper
                .getAllGrantedListsWithoutAdditionalRights(country, profile);
        List<ReglissListPermissionDetailsDto> defaultGrantedListsWithoutAdditionalRights = userMapper
                .getDefaultGrantedListsWithoutAdditionalRights(country, profile);
        allGrantedListsDto.allGrantedLists = allGrantedListsWithoutAdditionalRights.stream()
                .filter(list->defaultGrantedListsWithoutAdditionalRights.stream().noneMatch(dl->dl.listId.equals(list.listId)))
                .filter(list->assignedListPermissions.stream().noneMatch(al->al.getReglissListId().equals(list.listId)))
                .collect(toList());
        allGrantedListsDto.allGrantedLists.stream().filter(list->!list.writePermission)
                .forEach(list->list.setWritePermissionDisabled(true));
        allGrantedListsDto.confidentialities = userMapper.getConfidentialitiesForAllGrantedLists(allGrantedListsDto.allGrantedLists);
        return allGrantedListsDto;
    }

    public UserDto getForUpdate(Long id) {
        User user = repository.getExactlyOne(id);
        return userMapper.toDto(user);
    }

    public UserDetailsDto getDetailsForUpdate(Long id) {
        User user = repository.getExactlyOne(id);
        return userMapper.toUserDetailsDto(user);
    }

    public void export(UserSearchCriteria criteria, OutputStream outputStream) {
        auditService.logSearch(criteria);
        List<UserWithAccessibleListsVO> usersToExport = repository.searchForExport(criteria);
        searchExportService.export(usersToExport, outputStream);
    }

    public UserBreadcrumbDto getBreadcrumbForUser(long userId) {
        UserBreadcrumbVO user = repository.getBreadcrumbForUser(userId);
        UserBreadcrumbDto dto = userMapper.toUserBreadcrumbDto(user);
        return dto;
    }
}

