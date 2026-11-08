package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.facade.dto.administration.UserBreadcrumbDto;
import com.bnpp.regliss.facade.dto.administration.UserDetailsDto;
import com.bnpp.regliss.facade.dto.administration.UserDto;
import com.bnpp.regliss.facade.dto.administration.UserForUsersListDto;
import com.bnpp.regliss.facade.dto.list.ReglissListAllPermissionsAndAssignedListsDto;
import com.bnpp.regliss.facade.dto.list.ReglissListPermissionDetailsDto;
import com.bnpp.regliss.facade.dto.list.ReglissListPermissionDto;
import com.bnpp.regliss.repository.CountryRepository;
import com.bnpp.regliss.repository.ListPermissionMatrixRepository;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.service.permission.CurrentListPermissions;
import com.bnpp.regliss.service.permission.ListPermissionVO;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.vo.UserBreadcrumbVO;
import com.bnpp.regliss.vo.UserWithAccessibleListsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Service
public class UserMapper {

    @Autowired
    private ReglissListRepository reglissListRepository;

    @Autowired
    private CountryRepository countryRepo;

    @Autowired
    private ListPermissionMatrixRepository listPermissionMatrixRepository;

    @Autowired
    private CurrentListPermissions currentListPermissions;

    public UserDto toDto(User user) {
        UserDto dto = mapUserFields(user);
        dto.assignedLists = user.getAssignedListPermissions().stream()
                .map(this::toDto)
                .collect(toList());
        return dto;
    }

    public UserDetailsDto toUserDetailsDto(User user) { return mapUserDetailsFields(user); }

    public ReglissListPermissionDto toDto(UserListPermission permission) {
        ReglissListPermissionDto dto = new ReglissListPermissionDto();
        dto.listId = permission.getReglissListId();
        ReglissList reglissList = reglissListRepository.getExactlyOne(permission.getReglissListId());
        dto.confidentiality = reglissList.getConfidentiality();
        dto.listName = reglissList.getReference() + "-" + reglissList.getShortName();
        dto.permission = permission.getPermission();
        return dto;
    }

    public ReglissListPermissionDetailsDto toPermissionDetailsDto(UserListPermission permission) {
        ReglissListPermissionDetailsDto dto = new ReglissListPermissionDetailsDto();
        dto.listId = permission.getReglissListId();
        ReglissList reglissList = reglissListRepository.getExactlyOne(permission.getReglissListId());
        dto.isListDeleted = reglissList.isDeleted();
        dto.confidentiality = reglissList.getConfidentiality();
        dto.listName = reglissList.getReference() + "-" + reglissList.getShortName();
        dto.readPermission = permission.getPermission() == ListPermission.READ;
        dto.writePermission = permission.getPermission() == ListPermission.WRITE;
        dto.downloadImportPermission = permission.getPermission() == ListPermission.DOWNLOADIMPORT;
        dto.downloadExportPermission = permission.getPermission() == ListPermission.DOWNLOADEXPORT;
        return dto;
    }

    public ReglissListPermissionDto toDto(ListPermissionVO permission) {
        ReglissListPermissionDto dto = new ReglissListPermissionDto();
        dto.listId = permission.getReglissListId();
        ReglissList reglissList = reglissListRepository.getExactlyOne(permission.getReglissListId());
        dto.confidentiality = reglissList.getConfidentiality();
        dto.listName = reglissList.getReference() + "-" + reglissList.getShortName();
        dto.permission = permission.getPermission();
        return dto;
    }

    public ReglissListPermissionDetailsDto toPermissionDetailsDto(ListPermissionVO permission) {
        ReglissListPermissionDetailsDto dto = new ReglissListPermissionDetailsDto();
        dto.listId = permission.getReglissListId();
        ReglissList reglissList = reglissListRepository.getExactlyOne(permission.getReglissListId());
        dto.confidentiality = reglissList.getConfidentiality();
        dto.listName = reglissList.getReference() + "-" + reglissList.getShortName();
        dto.readPermission = permission.getPermission() == ListPermission.READ ;
        dto.writePermission = permission.getPermission() == ListPermission.WRITE ;
        dto.downloadImportPermission = permission.getPermission() == ListPermission.DOWNLOADIMPORT ;
        dto.downloadExportPermission = permission.getPermission() == ListPermission.DOWNLOADEXPORT ;
        return dto;
    }

    public UserDto toSearchResultDto(UserWithAccessibleListsVO vo) {
        UserDto dto = mapUserFields(vo.getUser());
        dto.allowedListsShortnames = vo.getListNames().stream().collect(joining(", "));
        return dto;
    }

    public UserForUsersListDto toUserForUsersListDto(UserWithAccessibleListsVO vo) {
        UserForUsersListDto dto = new UserForUsersListDto();
        dto.id = vo.getUser().getId();
        dto.username = vo.getUser().getUsername();
        dto.firstName = vo.getUser().getFirstName();
        dto.lastName = vo.getUser().getLastName();
        dto.profile = vo.getUser().getProfile();
        if(vo.getUser().getCountry() != null){
            dto.country = vo.getUser().getCountry().getId();
        }else{
            dto.country = null;
        }
        if (vo.getUser().hasExportedFileDownloadPermissionNow()) {
            dto.hasExportDownloadPermission = true;
            dto.exportDownloadPermissionEndDate = vo.getUser().getExportedFileDownloadPermissionEndDateOpt()
                    .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        }
        if (vo.getUser().hasImportedFileDownloadPermissionNow()) {
            dto.hasImportDownloadPermission = true;
            dto.importDownloadPermissionEndDate = vo.getUser().getImportedFileDownloadPermissionEndDateOpt()
                    .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        }
        dto.status = vo.getUser().getStatus();
        dto.allowedListsShortnames = vo.getListNames();
        dto.lastConnection = vo.getUser().getLastConnection();
        dto.dateOfUpdate = vo.getUser().getDateOfUpdate();
        dto.setHasAssignedLists(!vo.getUser().getAssignedListPermissions().isEmpty());
        return dto;
    }

    private UserDto mapUserFields(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.status = user.getStatus();
        dto.language = user.getLanguage();
        dto.country = user.getCountry() != null ? user.getCountry().getId() : null;
        dto.pole = user.getPole();
        dto.entity = user.getEntity();
        dto.businessLine = user.getBusinessLine();
        dto.email = user.getEmail();
        dto.profile = user.getProfile();
        if (user.hasPermission(FeaturePermission.userDownloadImportVersionPermissionAssignable)) {
            dto.userDownloadImportVersionPermissionAssignable = true;
        }
        if (user.hasPermission(FeaturePermission.userDownloadExportVersionPermissionAssignable)) {
            dto.userDownloadExportVersionPermissionAssignable = true;
        }

        if (user.hasExportedFileDownloadPermissionNow()) {
            dto.hasExportDownloadPermission = true;
            dto.exportDownloadPermissionEndDate = user.getExportedFileDownloadPermissionEndDateOpt()
                    .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        }
        if (user.hasImportedFileDownloadPermissionNow()) {
            dto.hasImportDownloadPermission = true;
            dto.importDownloadPermissionEndDate = user.getImportedFileDownloadPermissionEndDateOpt()
                    .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        }
        return dto;
    }

    private UserDetailsDto mapUserDetailsFields(User user) {
        UserDetailsDto dto = new UserDetailsDto();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();
        dto.status = user.getStatus();
        dto.language = user.getLanguage();
        dto.country = user.getCountry() != null ? user.getCountry().getId() : null;
        dto.pole = user.getPole();
        dto.entity = user.getEntity();
        dto.businessLine = user.getBusinessLine();
        dto.email = user.getEmail();
        dto.profile = user.getProfile();
        dto.lastConnection = user.getLastConnection();
        dto.dateOfUpdate = user.getDateOfUpdate();
        if (user.hasPermission(FeaturePermission.userDownloadImportVersionPermissionAssignable)) {
            dto.userDownloadImportVersionPermissionAssignable = true;
        }
        if (user.hasPermission(FeaturePermission.userDownloadExportVersionPermissionAssignable)) {
            dto.userDownloadExportVersionPermissionAssignable = true;
        }

        if (user.hasExportedFileDownloadPermissionNow()) {
            dto.hasExportDownloadPermission = true;
            dto.exportDownloadPermissionEndDate = user.getExportedFileDownloadPermissionEndDateOpt()
                    .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        }
        if (user.hasImportedFileDownloadPermissionNow()) {
            dto.hasImportDownloadPermission = true;
            dto.importDownloadPermissionEndDate = user.getImportedFileDownloadPermissionEndDateOpt()
                    .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        }
        return dto;
    }

    public User fromDtoForCreate(UserDto dto) {
        User user = new User();
        user.setUsername(dto.username);
        user.setFirstName(dto.firstName);
        user.setLastName(dto.lastName);
        user.setPole(dto.pole);
        user.setEntity(dto.entity);
        user.setBusinessLine(dto.businessLine);
        user.setLanguage(dto.language);
        if (dto.hasExportDownloadPermission) {
            user.setExportedFileDownloadPermissionEndDate(dto.exportDownloadPermissionEndDate);
        }
        if (dto.hasImportDownloadPermission) {
            user.setImportedFileDownloadPermissionEndDate(dto.importDownloadPermissionEndDate);
        }
        Country country = countryRepo.getExactlyOne(dto.country);
        user.setCountry(country);

        user.setEmail(dto.email);
        user.setProfile(dto.profile);
        user.setCountry(countryRepo.getExactlyOne(dto.country));
        user.setBusinessLine(dto.businessLine);
        user.setEmail(dto.email);
        user.setEntity(dto.entity);
        user.setLanguage(dto.language);
        user.setStatus(dto.status);
        user.setAssignedListPermissions(dto.assignedLists.stream()
                .map(this::toUserListPermission)
                .collect(toSet()));
        return user;
    }

    public User toEntity(User user, UserDto dto) {
        user.setLanguage(dto.language);
        user.setProfile(dto.profile);
        user.setStatus(dto.status);
        if (dto.hasExportDownloadPermission) {
            user.setExportedFileDownloadPermissionEndDate(dto.exportDownloadPermissionEndDate);
        } else {
            user.setExportedFileDownloadPermissionEndDate(null);
        }
        if (dto.hasImportDownloadPermission) {
            user.setImportedFileDownloadPermissionEndDate(dto.importDownloadPermissionEndDate);
        } else {
            user.setImportedFileDownloadPermissionEndDate(null);
        }
        Set<UserListPermission> newPermissions = dto.assignedLists.stream().map(this::toUserListPermission).collect(toSet());
        Set<UserListPermission> mergedPermissions = restoreUnchangedPermissions(user.getAssignedListPermissions(), newPermissions);
        user.setAssignedListPermissions(mergedPermissions);
        return user;
    }

    public User toEntity(User user, UserDetailsDto dto) {
        user.setAssignedListPermissions(createAssignedUserPermissionByProfile(user,dto));
        user.setLanguage(dto.language);
        user.setProfile(dto.profile);
        user.setStatus(dto.status);
        if (dto.hasExportDownloadPermission) {
            user.setExportedFileDownloadPermissionEndDate(dto.exportDownloadPermissionEndDate);
        } else {
            user.setExportedFileDownloadPermissionEndDate(null);
        }
        if (dto.hasImportDownloadPermission) {
            user.setImportedFileDownloadPermissionEndDate(dto.importDownloadPermissionEndDate);
        } else {
            user.setImportedFileDownloadPermissionEndDate(null);
        }
        user.setDateOfUpdate(dto.dateOfUpdate);

        return user;
    }
    private Set<UserListPermission> createAssignedUserPermissionByProfile(User user, UserDetailsDto dto) {
        List<ListPermissionVO> getDefaultListPermissions = currentListPermissions.getDefaultListPermissions(dto.profile, user.getCountry());
        Set<UserListPermission> newPermissions=new HashSet<>();
        if(user.getProfile() != dto.profile){
                newPermissions=updatePermissionWhenProfileChange(dto,getDefaultListPermissions);

        }else {
            if(user.getProfile().equals(Profile.ADMIN_MOA) || user.getProfile().equals(Profile.ADMIN_CL) ||
                    user.getProfile().equals(Profile.ADMIN_CG)) {
                newPermissions = updatePermissionWhenChangeImportExportDownloadPermission(user, dto, getDefaultListPermissions);
                if(!user.getAssignedListPermissions().isEmpty()) {
                    newPermissions.addAll(user.getAssignedListPermissions().stream().collect(toList()));
                }
            }
        }
        return  newPermissions;
    }
    public Set<UserListPermission> updatePermissionWhenProfileChange(UserDetailsDto dto, List<ListPermissionVO> defaultListPermissions) {
        Set<UserListPermission> newPermissions=new HashSet<>();
            if(dto.profile.equals(Profile.ADMIN_MOA) || dto.profile.equals(Profile.ADMIN_CL) || dto.profile.equals(Profile.ADMIN_CG)) {
                defaultListPermissions.stream().filter(p ->p.getPermission().equals(ListPermission.READ)).forEach(permission ->{
                        if (dto.hasExportDownloadPermission) {
                            newPermissions.add(createPermissionForUser(permission.getReglissListId(),ListPermission.DOWNLOADEXPORT));
                        }
                        if (dto.hasImportDownloadPermission) {
                            newPermissions.add(createPermissionForUser(permission.getReglissListId(),ListPermission.DOWNLOADIMPORT));
                        }
                }

                );
            }
            return  newPermissions;
    }
    public Set<UserListPermission> updatePermissionWhenChangeImportExportDownloadPermission(User user,UserDetailsDto dto,
                                                                                             List<ListPermissionVO> defaultListPermissions) {
        Set<UserListPermission> newPermissions=new HashSet<>();
        defaultListPermissions.stream().filter(p ->p.getPermission().equals(ListPermission.READ)).forEach(permission ->{
            if (dto.hasExportDownloadPermission && !user.hasExportedFileDownloadPermissionNow()) {
                newPermissions.add(createPermissionForUser(permission.getReglissListId(),ListPermission.DOWNLOADEXPORT));
            }
            if (dto.hasImportDownloadPermission && !user.hasImportedFileDownloadPermissionNow()) {
                newPermissions.add(createPermissionForUser(permission.getReglissListId(),ListPermission.DOWNLOADIMPORT));
            }
                }

        );
        return newPermissions;
    }
    private UserListPermission createPermissionForUser (long listId,ListPermission permission) {
        UserListPermission userGrant = new UserListPermission();
        userGrant.setPermission(permission);
        userGrant.setReglissListId(listId);
        return userGrant;
    }

    public User toUserEntity(User user, ReglissListAllPermissionsAndAssignedListsDto dto, Country country, Profile profile) {

        List<ReglissListPermissionDetailsDto> currentDefaultGrantedLists = getDefaultGrantedListsWithoutAdditionalRights(country,profile);
        List<ReglissListPermissionDetailsDto> newDefaultGrantedListsPermissions = getNewListsPermissions(dto.defaultGrantedLists,
currentDefaultGrantedLists);

        List<ReglissListPermissionDetailsDto> allNewPermissions = new ArrayList<>();
        allNewPermissions.addAll(dto.assignedLists);
        allNewPermissions.addAll(newDefaultGrantedListsPermissions);
        Set<UserListPermission> newPermissions = mapListsPermissions(allNewPermissions);
        Set<UserListPermission> mergedPermissions = restoreUnchangedPermissions(user.getAssignedListPermissions(), newPermissions);
        user.setAssignedListPermissions(mergedPermissions);

        return user;
    }

    private List<ReglissListPermissionDetailsDto> getNewListsPermissions(List<ReglissListPermissionDetailsDto> newPerms,
                                                                                       List<ReglissListPermissionDetailsDto> currentPerms){
        return newPerms.stream().map(newPerm -> currentPerms.stream()
                .filter(oldPerm -> oldPerm.listId.equals(newPerm.listId)
                        && oldPerm.confidentiality.equals(newPerm.confidentiality)
                        && oldPerm.listName.equals(newPerm.listName)
                        && (oldPerm.readPermission != newPerm.readPermission
                        || oldPerm.writePermission != newPerm.writePermission
                        || oldPerm.downloadImportPermission != newPerm.downloadImportPermission
                        || oldPerm.downloadExportPermission != newPerm.downloadExportPermission))
                .map(oldPerm -> {
                    oldPerm.readPermission = newPerm.readPermission != oldPerm.readPermission && newPerm.readPermission;
                    oldPerm.writePermission = newPerm.writePermission != oldPerm.writePermission && newPerm.writePermission;
                    oldPerm.downloadImportPermission = newPerm.downloadImportPermission != oldPerm.downloadImportPermission
                            && newPerm.downloadImportPermission;
                    oldPerm.downloadExportPermission = newPerm.downloadExportPermission != oldPerm.downloadExportPermission
                            && newPerm.downloadExportPermission;
                    return oldPerm;
            }).collect(Collectors.toList())).flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<ReglissListPermissionDetailsDto> getAllGrantedListsWithoutAdditionalRights(Country country, Profile profile){
        return currentListPermissions.getAssignableListPermissions(profile, country).stream()
                .map(this::toPermissionDetailsDto)
                .collect(Collectors.toMap(ReglissListPermissionDetailsDto::getListId, ReglissListPermissionDetailsDto::new,
                        ReglissListPermissionDetailsDto::merge))
                .values().stream().collect(toList());
    }

    public List<ReglissListPermissionDetailsDto> getDefaultGrantedListsWithoutAdditionalRights(Country country, Profile profile){
        return currentListPermissions.getDefaultListPermissions(profile, country).stream()
                .map(this::toPermissionDetailsDto)
                .collect(Collectors.toMap(ReglissListPermissionDetailsDto::getListId, ReglissListPermissionDetailsDto::new,
                        ReglissListPermissionDetailsDto::merge))
                .values().stream().collect(toList());
    }

    public List<ReglissListPermissionDetailsDto> getAllListsAdditionalRights(User user, Profile profile, Country country){
        return currentListPermissions.getAssignableListPermissions(profile,country).stream()
                .map(this::toPermissionDetailsDto)
                .map(listFromPerimeter ->
                        user.getAssignedListPermissions()
                                .stream()
                                .map(this::toPermissionDetailsDto)
                                .filter(additionalRight -> additionalRight.getListId().equals(listFromPerimeter.getListId()))
                                .collect(Collectors.toList())
                ).flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<ReglissListPermissionDetailsDto> getDefaultListsAdditionalRights(User user, Profile profile, Country country){
        return currentListPermissions.getDefaultListPermissions(profile,country).stream()
                .map(this::toPermissionDetailsDto)
                .map(listFromPerimeter ->
                        user.getAssignedListPermissions()
                                .stream()
                                .map(this::toPermissionDetailsDto)
                                .filter(additionalRight -> additionalRight.getListId().equals(listFromPerimeter.getListId()))
                                .collect(Collectors.toList())
                ).flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<ReglissListPermissionDetailsDto> getAllGrantedListsWithAdditionalRights(
            List<ReglissListPermissionDetailsDto> allGrantedListsWithoutAdditionalRights,
              List<ReglissListPermissionDetailsDto> additionalRightsForAllGrantedLists){
        List<ReglissListPermissionDetailsDto> allGrantedListsWithAdditionalRights = new ArrayList<>();
        allGrantedListsWithAdditionalRights.addAll(allGrantedListsWithoutAdditionalRights);
        allGrantedListsWithAdditionalRights.addAll(additionalRightsForAllGrantedLists);

        return allGrantedListsWithAdditionalRights.stream()
                .collect(Collectors.toMap(ReglissListPermissionDetailsDto::getListId, ReglissListPermissionDetailsDto::new,
                        ReglissListPermissionDetailsDto::merge))
                .values().stream().collect(toList());
    }

    public List<ReglissListPermissionDetailsDto> getDefaultGrantedListsWithAdditionalRights(
            List<ReglissListPermissionDetailsDto> defaultGrantedListsWithoutAdditionalRights,
             List<ReglissListPermissionDetailsDto> additionalRightsForDefaultGrantedLists){
        List<ReglissListPermissionDetailsDto> defaultGrantedListsWithAdditionalRights = new ArrayList<>();
        defaultGrantedListsWithoutAdditionalRights.stream().filter(list-> list.readPermission)
                .forEach(list->list.setReadPermissionDisabled(true));
        defaultGrantedListsWithoutAdditionalRights.stream().filter(list->list.writePermission)
                .forEach(list->list.setWritePermissionDisabled(true));
        defaultGrantedListsWithAdditionalRights.addAll(defaultGrantedListsWithoutAdditionalRights);
        defaultGrantedListsWithAdditionalRights.addAll(additionalRightsForDefaultGrantedLists);

        return defaultGrantedListsWithAdditionalRights.stream()
                .collect(Collectors.toMap(ReglissListPermissionDetailsDto::getListId, ReglissListPermissionDetailsDto::new,
                        ReglissListPermissionDetailsDto::merge))
                .values().stream().collect(toList());
    }

    public Set<UserListPermission> getAdditionalLists(User user, List<ReglissListPermissionDetailsDto> listsFromPerimeter){
        Set<UserListPermission> additionalListsPermissions = new HashSet<>();
        List<Long> listsIdsFromPerimeter = new ArrayList<>();
        listsFromPerimeter.forEach(p->listsIdsFromPerimeter.add(p.listId));
        for (UserListPermission userPerm : user.getAssignedListPermissions()) {
            if (!listsIdsFromPerimeter.contains(userPerm.getReglissListId())) {
                additionalListsPermissions.add(userPerm);
            }
        }
        return additionalListsPermissions;
    }

    private Set<UserListPermission> mapListsPermissions(List<ReglissListPermissionDetailsDto> listsPermissions) {
        return listsPermissions.stream().map(l -> {
            Map<ListPermission, Boolean> permissions = new EnumMap<>(ListPermission.class);
            permissions.put(ListPermission.READ, l.readPermission);
            permissions.put(ListPermission.WRITE, l.writePermission);
            permissions.put(ListPermission.DOWNLOADIMPORT, l.downloadImportPermission);
            permissions.put(ListPermission.DOWNLOADEXPORT, l.downloadExportPermission);
            return permissions.entrySet().stream().filter(Map.Entry::getValue).map(p -> {
                ReglissListPermissionDetailsDto newList = new ReglissListPermissionDetailsDto(l.listId, l.confidentiality, l.listName);
                switch (p.getKey()) {
                    case READ:
                        newList.readPermission = true;
                        break;
                    case WRITE:
                        newList.writePermission = true;
                        break;
                    case DOWNLOADIMPORT:
                        newList.downloadImportPermission = true;
                        break;
                    case DOWNLOADEXPORT:
                        newList.downloadExportPermission = true;
                        break;
                    default:
                        break;
                }
                return newList;
            }).collect(Collectors.toList());
        }).flatMap(Collection::stream).collect(Collectors.toList()).stream().map(this::toUserListPermissionDetails).collect(toSet());
    }

    private Set<UserListPermission> restoreUnchangedPermissions(Set<UserListPermission> oldPerms,
                                                                Set<UserListPermission> newPerms) {
        return newPerms.stream().map(newPerm -> {
            Optional<UserListPermission> oldIdentical = oldPerms.stream()
                    .filter(oldPerm -> oldPerm.getPermission() == newPerm.getPermission()
                            && oldPerm.getReglissListId().equals(newPerm.getReglissListId()))
                    .findFirst();
            return oldIdentical.orElse(newPerm);
        }).collect(toSet());
    }

    public UserListPermission toUserListPermission(ReglissListPermissionDetailsDto dto) {
        UserListPermission userGrant = new UserListPermission();
        userGrant.setPermission(dto.permission);
        userGrant.setReglissListId(dto.listId);
        return userGrant;
    }

    public UserListPermission toUserListPermissionDetails(ReglissListPermissionDetailsDto dto) {
        UserListPermission userGrant = new UserListPermission();
        if (dto.readPermission) {
            userGrant.setPermission(ListPermission.READ);
        } else if (dto.writePermission) {
            userGrant.setPermission(ListPermission.WRITE);
        } else if (dto.downloadImportPermission) {
            userGrant.setPermission(ListPermission.DOWNLOADIMPORT);
        } else if (dto.downloadExportPermission) {
            userGrant.setPermission(ListPermission.DOWNLOADEXPORT);
        }
        userGrant.setReglissListId(dto.listId);
        return userGrant;
    }

    public UserBreadcrumbDto toUserBreadcrumbDto(UserBreadcrumbVO user) {
        UserBreadcrumbDto dto = new UserBreadcrumbDto();
        dto.idUser = user.getId();
        dto.username = user.getUsername();
        dto.firstName = user.getFirstName();
        dto.lastName = user.getLastName();

        return dto;
    }

    public List<ReglissListPermissionDetailsDto> mapAssignedLists(Set<UserListPermission> additionalListsPermissions) {
        return additionalListsPermissions.stream()
                .map(this::toPermissionDetailsDto)
                .filter(this::reglissListIsActive)
                .collect(Collectors.toMap(ReglissListPermissionDetailsDto::getListId, ReglissListPermissionDetailsDto::new,
                        ReglissListPermissionDetailsDto::merge))
                .values().stream().collect(toList());
    }

    private boolean reglissListIsActive(ReglissListPermissionDetailsDto permissionDetailsDto){
        return !permissionDetailsDto.isListDeleted();
    }

    public Set<Confidentiality> getConfidentialitiesForAllGrantedLists(List<ReglissListPermissionDetailsDto> allGrantedLists){
        Set<Confidentiality> confidentialities = new HashSet<>();
        allGrantedLists.stream().forEach(l->confidentialities.add(l.getConfidentiality()));

        return confidentialities;
    }
}

