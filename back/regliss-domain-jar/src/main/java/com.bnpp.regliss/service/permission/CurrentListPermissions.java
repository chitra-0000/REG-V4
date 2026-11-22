package com.bnpp.regliss.service.permission;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.repository.ListPermissionMatrixRepository;
import com.bnpp.regliss.repository.ReglementationRepository;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.service.ReglissRequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Service
public class CurrentListPermissions {

    @Autowired
    private ListPermissionMatrixRepository listPermissionMatrixRepository;

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ReglementationRepository reglementationRepo;

    public boolean isVersionReadOnly(ListPermissionRequest list) {
        return requestContext.isLowAuthentication() || getCurrentPermissionForList(list) == ListPermission.READ;
    }

    public boolean isListReadOnly() {
        return requestContext.isLowAuthentication();
    }

    public void checkCanModifyVersion(long listId) {
        if (!canModifyVersion(listRepo.getListPermissionRequest(listId)))  {
            throw new ReglissException(ErrorCode.NO_PERMISSION_FOR_LIST);
        }
    }

    public boolean canModifyVersion(ListPermissionRequest permissionRequest) {
        boolean hasWritePermission = getCurrentPermissionForList(permissionRequest) == ListPermission.WRITE;
        return requestContext.isHighAuthentication() && hasWritePermission;
    }

    public void checkListAccess(long listId) {
        checkListAccess(listRepo.getListPermissionRequest(listId));
    }

    public void checkListAccess(ListPermissionRequest permissionRequest) {
        boolean isConfidential = permissionRequest.getConfidentiality() == Confidentiality.CONFIDENTIAL;
        if (!requestContext.isHighAuthentication() && isConfidential) {
            throw new ReglissException(ErrorCode.AUTHENTICATION_HIGH_LEVEL_REQUIRED);
        }
        if (getCurrentPermissionForList(permissionRequest) == null) {
            throw new ReglissException(ErrorCode.NO_PERMISSION_FOR_LIST);
        }
    }
    public List<ListPermissionVO> getAssignableListPermissions(Profile profile, Country userCountry){
        List<ListPermissionRequest> allLists = listRepo.findAllNotDeletedEagerForPermissions();
        List<ListPermissionMatrix> permissionMatrixByProfile = listPermissionMatrixRepository.findByProfile(profile);
        return allLists.stream()
                    .flatMap(listPermRequest -> mapAssignableListPermissions(permissionMatrixByProfile, listPermRequest, userCountry).stream())
                    .collect(toList());
    }

    public List<ListPermissionVO> getDefaultListPermissions(Profile profile, Country userCountry){
        List<ListPermissionRequest> allLists = listRepo.findAllNotDeletedEagerForPermissions();
        List<ListPermissionMatrix> permissionMatrixByProfile = listPermissionMatrixRepository.findByProfile(profile);
        return allLists.stream()
                    .flatMap(listPermRequest -> mapDefaultListPermissions(permissionMatrixByProfile, listPermRequest, userCountry).stream())
                    .collect(toList());
    }

    private List<ListPermissionVO> mapAssignableListPermissions(List<ListPermissionMatrix> permissionMatrixByProfile, ListPermissionRequest list, Country userCountry) {
        return filterPermissionsForList(permissionMatrixByProfile, list, userCountry).stream()
                .filter(listPermission -> listPermission.getAssignablePerm() != null)
                .map(listPermission -> new ListPermissionVO(list.getId(), listPermission.getAssignablePerm()))
                .collect(toList());
    }

    private List<ListPermissionVO> mapDefaultListPermissions(List<ListPermissionMatrix> permissionMatrixByProfile, ListPermissionRequest list, Country userCountry) {
        return filterPermissionsForList(permissionMatrixByProfile, list, userCountry).stream()
                .filter(listPermission -> listPermission.getDefaultPerm() != null)
                .map(listPermission -> new ListPermissionVO(list.getId(), listPermission.getDefaultPerm()))
                .collect(toList());
    }

    private List<ListPermissionMatrix> filterPermissionsForList(List<ListPermissionMatrix> permissionsMatrix, ListPermissionRequest reglissList, Country userCountry){
        Boolean isSameCountry = reglissList.getCountryId().equals(userCountry.getId());

        return permissionsMatrix.stream()
                    .filter(perm -> perm.getConfidentiality() == reglissList.getConfidentiality()
                                    && (perm.getScope() == null || perm.getScope().equals(reglissList.getScope()))
                                    && (perm.getSameCountry() == null || isSameCountry.equals(perm.getSameCountry())))
                    .collect(toList());
    }

    public Set<Long> getAllowedListsIdsForCurrentUser() {
        User user = requestContext.getCurrentUser();
        Set<Long> allowedListsIds = new HashSet<>();

        getDefaultListPermissions(user.getProfile(), user.getCountry())
                .stream()
                .map(ListPermissionVO::getReglissListId)
                .forEach(allowedListsIds::add);

        user.getAssignedListPermissions()
                .stream()
                .map(UserListPermission::getReglissListId)
                .forEach(allowedListsIds::add);

        return allowedListsIds;
    }

    public ListPermission getCurrentPermissionForList(ListPermissionRequest list) {
        if (list == null) {
            return null;
        }
        User user = requestContext.getCurrentUser();
        List<ListPermissionMatrix> permissionMatrixByProfile = listPermissionMatrixRepository.findByProfile(user.getProfile());
        List<ListPermission> permissionsForList = mapDefaultListPermissions(permissionMatrixByProfile, list, user.getCountry())
                                                          .stream()
                                                          .map(ListPermissionVO::getPermission)
                                                          .collect(toList());
        user.getAssignedListPermissions().stream()
            .filter(perm -> list.getId().equals(perm.getReglissListId()))
            .map(UserListPermission::getPermission)
            .forEach(permissionsForList::add);
        if (permissionsForList.contains(ListPermission.WRITE)) {
            return ListPermission.WRITE;
        }
        if (permissionsForList.contains(ListPermission.READ)) {
            return ListPermission.READ;
        }
        return null;
    }
    public boolean hasReadPermissionForList(User user,ListPermissionRequest list) {
        List<ListPermissionMatrix> permissionMatrixByProfile = listPermissionMatrixRepository.findByProfile(user.getProfile());
        List<ListPermission> permissionsForList = mapDefaultListPermissions(permissionMatrixByProfile, list, user.getCountry())
                .stream()
                .map(ListPermissionVO::getPermission)
                .collect(toList());
        return permissionsForList.contains(ListPermission.READ);
    }
    public void checkListReglementationAccess(Long reglementationId) {
        Reglementation reglementation = reglementationRepo.getExactlyOne(reglementationId);
        ReglissList list = reglementation.getListOpt()
                .orElseThrow(() -> new IllegalArgumentException("List should be present for reglementation"));
        checkListAccess(list.getId());
    }


}
