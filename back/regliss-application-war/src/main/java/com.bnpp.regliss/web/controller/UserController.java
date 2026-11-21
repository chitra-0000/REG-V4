package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.entity.Language;
import com.bnpp.regliss.entity.ListPermission;
import com.bnpp.regliss.entity.Profile;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.facade.UserFacade;
import com.bnpp.regliss.facade.dto.LoggedInUserDto;
import com.bnpp.regliss.facade.dto.SingleValue;
import com.bnpp.regliss.facade.dto.administration.UserBreadcrumbDto;
import com.bnpp.regliss.facade.dto.administration.UserDetailsDto;
import com.bnpp.regliss.facade.dto.administration.UserDto;
import com.bnpp.regliss.facade.dto.administration.UserForUsersListDto;
import com.bnpp.regliss.facade.dto.list.AllGrantedListsDto;
import com.bnpp.regliss.facade.dto.list.ReglissListAllPermissionsAndAssignedListsDto;
import com.bnpp.regliss.facade.dto.list.ReglissListAllPermissionsDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.search.UserSearchCriteria;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.web.filter.SecuredFeature;
import com.bnpp.regliss.web.security.MyUserWithContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/user")
@ReglissWebProfile
public class UserController {

    @Autowired
    private UserFacade facade;

    @Value("${file.user.guide.path}")
    private String userGuidePath;

    @GetMapping("current")
    public LoggedInUserDto getLoggedInUser() {
        return facade.getLoggedInUser();
    }

    @PutMapping("current")
    public void updateLastConnexionDate() {
        facade.updateLastConnectionDate();
    }

    @PostMapping("current/language/{lang}")
    public void savePreferences(@PathVariable("lang") Language language) {
        facade.saveLanguage(language);
        getUserFromSecurity().ifPresent(user -> user.setLanguage(language));
    }

    @SuppressWarnings("unchecked")
    private Optional<User> getUserFromSecurity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Optional.empty();
        }
        MyUserWithContext<User> securityUser = (MyUserWithContext<User>) auth.getPrincipal();
        User userFromSecurity = securityUser.getUser();
        return Optional.ofNullable(userFromSecurity);
    }

    @SecuredFeature(FeaturePermission.searchUsers)
    @PostMapping("search")
    public PaginatedResults<UserDto> search(@RequestBody UserSearchCriteria criteria) {
        return facade.search(criteria);
    }

    @GetMapping("/get-users-list")
    public List<UserForUsersListDto> getUsersList() {
        return facade.getUserForUsersList();
    }

    @SecuredFeature(FeaturePermission.manageUsers)
    @GetMapping("add-user-search-refog")
    public UserDto searchUserInRefog(@RequestParam("username") String username) {
        return facade.searchRefogForCreate(username);
    }

    @SecuredFeature(FeaturePermission.searchUsers)
    @GetMapping("get-grantable-lists")
    public ReglissListAllPermissionsDto getPermissions(@RequestParam(name = "country") long countryId, @RequestParam(name = "profile") Profile profile) {
        return facade.getListPermissionsForUser(profile, countryId);
    }

    @SecuredFeature(FeaturePermission.searchUsers)
    @GetMapping("get-grantable-lists-and-assigned-lists")
    public ReglissListAllPermissionsAndAssignedListsDto getPermissionsDetails(@RequestParam(name = "id") long userId, @RequestParam(name = "country") long countryId,
                                                                              @RequestParam(name = "profile") Profile profile) {
        return facade.getListPermissionsAndAssignedListsForUser(userId, profile, countryId);
    }

    @SecuredFeature(FeaturePermission.searchUsers)
    @GetMapping("get-all-grantable-lists")
    public AllGrantedListsDto getAllGrantedLists(@RequestParam(name = "id") long userId, @RequestParam(name = "country") long countryId, @RequestParam(name = "profile") Profile profile) {
        return facade.getAllGrantedLists(userId, profile, countryId);
    }

    @SecuredFeature(FeaturePermission.manageUsers)
    @PostMapping
    public SingleValue create(@RequestBody UserDto userDto) {
        long newId = facade.create(userDto);
        return new SingleValue(newId);
    }

    @SecuredFeature(FeaturePermission.searchUsers)
    @GetMapping("/{userId}")
    public UserDto getUserById(@PathVariable("userId") Long id) {
        return facade.getForUpdate(id);
    }

    @SecuredFeature(FeaturePermission.searchUsers)
    @GetMapping("/edit-user/{userId}")
    public UserDetailsDto getUserDetailsById(@PathVariable("userId") Long id) {
        return facade.getDetailsForUpdate(id);
    }

    @SecuredFeature(FeaturePermission.manageUsers)
    @PutMapping
    public void updateUser(@RequestBody UserDto userDto) {
        facade.update(userDto);
    }

    @SecuredFeature(FeaturePermission.manageUsers)
    @PutMapping("/edit-user")
    public void updateUserDetails(@RequestBody UserDetailsDto userDetailsDto) {
        facade.updateDetails(userDetailsDto);
    }

    @SecuredFeature(FeaturePermission.manageUsers)
    @PutMapping("/grantable-lists-and-assigned-lists")
    public void updateUserPermissionsAndAssignedLists(@RequestParam(name = "id") long userId, @RequestParam(name = "country") long countryId,
                                                      @RequestParam(name = "profile") Profile profile,
                                                      @RequestBody ReglissListAllPermissionsAndAssignedListsDto reglissListAllPermissionsAndAssignedListsDto) {
        facade.updatePermissionsAndAssignedLists(userId, countryId, profile, reglissListAllPermissionsAndAssignedListsDto);
    }

    @SecuredFeature(FeaturePermission.manageUsers)
    @PutMapping("/remove-user-download-permissions")
    public void removeUserDownloadPermissions(@RequestParam (name = "id") long userId, @RequestParam (name = "downloadType") ListPermission downloadType) {
        facade.removeDownloadPermissions(userId, downloadType);
    }

    @SecuredFeature(FeaturePermission.searchUsers)
    @GetMapping("search/export")
    public void searchUserExport(@RequestParam("data") UserSearchCriteria criteria, HttpServletResponse response) throws IOException {
        HttpResponseUtil.prepareResponseWithCsv("Export-Users", response);
        facade.export(criteria, response.getOutputStream());
    }

    @GetMapping("/breadcrumb-for-user/{id}")
    public UserBreadcrumbDto getBreadcrumbForList(@PathVariable Long id) {
        return facade.getBreadcrumbForUser(id);
    }

}

