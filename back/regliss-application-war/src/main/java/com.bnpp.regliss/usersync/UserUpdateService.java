package com.bnpp.regliss.usersync;

import com.bnpp.regliss.client.refog.RefogClient;
import com.bnpp.regliss.client.refog.RefogUser;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.CountryRepository;
import com.bnpp.regliss.repository.UserRepository;
import com.bnpp.regliss.service.UserAuditService;
import com.bnpp.regliss.usersync.dto.UserDifferenceMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@ReglissWebProfile
@Service
@Transactional(propagation = Propagation.REQUIRED, timeout = 100)
public class UserUpdateService {

    @Autowired
    private RefogClient refogClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CountryRepository countryRepo;

    @Autowired
    private CsvWriter csvWriter;

    @Autowired
    private UserAuditService auditService;

    @Value("${user.lastConnection.inactive.days}")
    private long inActiveDays;

    @Value("${user.ignore.profiles}")
    private List<String> userIgnoreProfiles;

    List<User> usersToBeDeactivated = new ArrayList<>();
    List<User> deactivatedUsers = new ArrayList<>();
    List<User> usersToBeModified = new ArrayList<>();
    List<User> modifiedUsers = new ArrayList<>();

    public void getUsersFromRefog() throws IOException, MessagingException {
        try {
            clear();
            List<User> allUsersInRepo = userRepository.findAll();
            log.info("All users count : {} ", allUsersInRepo.size());

            List<User> activeAndNonIgnoredUsers = findActiveAndNonIgnoredUsers(allUsersInRepo);
            log.info("Active and non ignored users count : {} ", activeAndNonIgnoredUsers.size());

            List<String> activeUIDs = activeAndNonIgnoredUsers.stream().map(User::getUsername).collect(Collectors.toList());
            log.info("Active UIDs count : {}", activeUIDs.size());

            List<RefogUser> refogUsers = refogClient.getUsersByUsernames(activeUIDs);
            log.info("Refog users count : {}", refogUsers.size());

            List<User> goToInactivateUsers = findUsersNeedstoBeDeactivated(activeAndNonIgnoredUsers, refogUsers);

            deactivatingDetectedUsers(goToInactivateUsers);

            List<UserDifferenceMapper> deactivatedUsersExcel = new ArrayList<>();
            for (User user : deactivatedUsers) {
                UserDifferenceMapper userDeactivationMapper = new UserDifferenceMapper();
                deactivatedUsersExcel.add(updatingExcelSheetForDeactivatedUsers(userDeactivationMapper, user));
            }

            for (User user : deactivatedUsers) {
                activeAndNonIgnoredUsers.remove(user);
            }

            List<UserDifferenceMapper> modifiedUsersExcel = getUserDifference(activeAndNonIgnoredUsers, refogUsers);
            log.info("Users needs to be modified : {} , Modified users in db : {}", usersToBeModified.size(), modifiedUsers.size());

            List<UserDifferenceMapper> totalExcelUsers = Stream.concat(modifiedUsersExcel.stream(), deactivatedUsersExcel.stream()).distinct().collect(Collectors.toList());
            log.info("Total no of users to be updated by batch {}", totalExcelUsers.size());

            writeExcelSheet(totalExcelUsers, deactivatedUsersExcel, modifiedUsersExcel);

            List<User> oldUsers = Stream.concat(usersToBeDeactivated.stream(), usersToBeModified.stream()).distinct().collect(Collectors.toList());
            List<User> newUsers = Stream.concat(deactivatedUsers.stream(), modifiedUsers.stream()).distinct().collect(Collectors.toList());
            updateAudit(oldUsers, newUsers);
        } finally {
            clear();
        }
    }

    private void clear() {
        usersToBeModified.clear();
        modifiedUsers.clear();
        usersToBeDeactivated.clear();
        deactivatedUsers.clear();
    }

    private void writeExcelSheet(List<UserDifferenceMapper> totalExcelUsers, List<UserDifferenceMapper> deactivatedUsersExcel, List<UserDifferenceMapper> modifiedUsersExcel) throws IOException {
        Map<Integer, String> testData = new HashMap<>();
        testData.put(1, "UID;Profile;Post batch status;Last connection;First Name (Previous Value in REGLISS);First Name(New value in REFOG);Last Name(Previous Value in REGLISS);" +
                "Last Name(New value in REFOG);Country (Previous Value in REGLISS);Country (New value in REFOG);Pole (Previous Value in REGLISS);Pole (New value in REFOG);" +
                "Entity (Previous Value in REGLISS);Entity(New value in REFOG);Business Line(Previous Value in REGLISS);Business Line(New value in REFOG);Email(Previous Value in REGLISS);" +
                "Email(New value in REFOG);Batch update date");
        if (!(totalExcelUsers.isEmpty())) {
            Integer i = 1;
            for (UserDifferenceMapper user : totalExcelUsers) {
                i = i + 1;
                testData.put(i, (user.getUid() + ";" + user.getProfile() + ";" + user.getPostBatchStatus() + ";" + user.getLastConnection()
                        + ";" + user.getFirstNamePreviousValueInRegliss() + ";" + user.getFirstNameNewValueInRegliss()
                        + ";" + user.getLastNamePreviousValueInRegliss() + ";" + user.getLastNameNewValueInRegliss()
                        + ";" + user.getCountryPreviousValueinRegliss() + ";" + user.getCountryNewValueinRegliss()
                        + ";" + user.getPolePreviousValueinRegliss() + ";" + user.getPoleNewValueinRegliss()
                        + ";" + user.getEntityPreviousValueinRegliss() + ";" + user.getEntityNewValueinRegliss()
                        + ";" + user.getBusinesLinePreviousValueinRegliss() + ";" + user.getBusinessLineNewValueinRegliss()
                        + ";" + user.getEmailPreviousValueinRegliss() + ";" + user.getEmailNewValueinRegliss()
                        + ";" + LocalDate.now()));
            }
            csvWriter.writeExcelSheet(testData, deactivatedUsersExcel.size(), modifiedUsersExcel.size());
        } else {
            csvWriter.sendMail(null, deactivatedUsersExcel.size(), modifiedUsersExcel.size());
        }
    }

    public List<UserDifferenceMapper> getUserDifference(List<? extends User> users, List<RefogUser> refogUserList) {
        List<UserDifferenceMapper> userDifferenceMappers = new ArrayList<>();
        for (User dbUser : users) {
            for (RefogUser refogUser : refogUserList) {
                if (dbUser.getUsername().equals(refogUser.getUsername())) {
                    userDifferenceMappers.add(calculateUserstoBeModified(dbUser, refogUser));
                }
            }
        }
        return userDifferenceMappers.stream().filter(a -> a != null).collect(Collectors.toList());
    }

    private void updateAudit(List<User> oldUsers, List<User> newUsers) {
        if (!oldUsers.isEmpty()) {
            for (User newUser : newUsers) {
                for (User oldUser : oldUsers) {
                    if (oldUser.getUsername().equalsIgnoreCase(newUser.getUsername())) {
                        auditService.auditUpdate(oldUser, newUser);
                    }
                }
            }
        }
    }

    private UserDifferenceMapper calculateUserstoBeModified(User dbUser, RefogUser refogUser) {
        if (dbUser.getProfile() != null) {
            String profile = dbUser.getProfile().toString();
            compareAndSetEmptyRefogFields(refogUser);
            if ((profile.equalsIgnoreCase("SUPER_PROFILE") || profile.equalsIgnoreCase("READER") || profile.equalsIgnoreCase("ADMIN_TECHNICAL"))
                    && (!compareAllFieldsOfTwoObjects(dbUser, refogUser))) {
                usersToBeModifyMapping(dbUser);
                UserDifferenceMapper userDifferenceMapper = updatingExcelSheetForModifiedUsers(dbUser, refogUser);
                updatingAllModifiedFieldsInDb(dbUser, refogUser);
                return userDifferenceMapper;
            } else if ((profile.equalsIgnoreCase("ADMIN_CG") || profile.equalsIgnoreCase("ADMIN_CL") || profile.equalsIgnoreCase("ADMIN_MOA") ||
                    profile.equalsIgnoreCase("ADMIN_SUPPORT") || profile.equalsIgnoreCase("ADMIN_CR")) && (!compareAllFieldsOfTwoObjects(dbUser, refogUser))) {
                usersToBeModifyMapping(dbUser);
                UserDifferenceMapper userDifferenceMapper = updatingExcelSheetForModifiedUsers(dbUser, refogUser);
                updatingModifiedFieldsInDb(dbUser, refogUser);
                return userDifferenceMapper;
            }
        }
        return null;
    }

    private UserDifferenceMapper updatingExcelSheetForModifiedUsers(User dbUser, RefogUser refogUser) {
        UserDifferenceMapper updateExcel = new UserDifferenceMapper();
        updateExcel.setUid(dbUser.getUsername());
        updateExcel.setFirstNamePreviousValueInRegliss(dbUser.getFirstName());
        updateExcel.setFirstNameNewValueInRegliss(refogUser.getFirstName());
        updateExcel.setLastNamePreviousValueInRegliss(dbUser.getLastName());
        updateExcel.setLastNameNewValueInRegliss(refogUser.getLastName());
        updateExcel.setProfile(dbUser.getProfile().name());
        if (dbUser.getStatus() != null)
            updateExcel.setPostBatchStatus(dbUser.getStatus().name());
        else updateExcel.setPostBatchStatus(null);
        if (dbUser.getLastConnection() != null)
            updateExcel.setLastConnection(dbUser.getLastConnection().toString());
        else updateExcel.setLastConnection(null);
        if (dbUser.getCountry() != null)
            updateExcel.setCountryPreviousValueinRegliss(getCountryName(dbUser.getCountry()));
        else updateExcel.setCountryPreviousValueinRegliss(null);
        updateExcel.setCountryNewValueinRegliss(refogUser.getCountryName());
        updateExcel.setBusinesLinePreviousValueinRegliss(dbUser.getBusinessLine());
        updateExcel.setBusinessLineNewValueinRegliss(refogUser.getBusinessLine());
        updateExcel.setPolePreviousValueinRegliss(dbUser.getPole());
        updateExcel.setPoleNewValueinRegliss(refogUser.getPole());
        updateExcel.setEntityPreviousValueinRegliss(dbUser.getEntity());
        updateExcel.setEntityNewValueinRegliss(refogUser.getEntity());
        updateExcel.setEmailPreviousValueinRegliss(dbUser.getEmail());
        updateExcel.setEmailNewValueinRegliss(refogUser.getEmail());
        return updateExcel;
    }

    private void updatingAllModifiedFieldsInDb(User dbUser, RefogUser refogUser) {
        if (!StringUtils.equalsIgnoreCase(dbUser.getFirstName(), refogUser.getFirstName())) {
            dbUser.setFirstName(refogUser.getFirstName());
        }
        if (!StringUtils.equalsIgnoreCase(dbUser.getLastName(), refogUser.getLastName())) {
            dbUser.setLastName(refogUser.getLastName());
        }
        if (!StringUtils.equalsIgnoreCase(dbUser.getEmail(), refogUser.getEmail())) {
            dbUser.setEmail(refogUser.getEmail());
        }
        if (!StringUtils.equalsIgnoreCase(dbUser.getEntity(), refogUser.getEntity())) {
            dbUser.setEntity(refogUser.getEntity());
        }
        if (!StringUtils.equalsIgnoreCase(dbUser.getPole(), refogUser.getPole())) {
            dbUser.setPole(refogUser.getPole());
        }
        if (!StringUtils.equalsIgnoreCase(dbUser.getBusinessLine(), refogUser.getBusinessLine())) {
            dbUser.setBusinessLine(refogUser.getBusinessLine());
        }
        if (!StringUtils.equalsIgnoreCase(getCountryName(dbUser.getCountry()), refogUser.getCountryName())) {
            dbUser.setCountry(countryRepo.findByLabelIgnoreCaseAndV3OnlyFalse(refogUser.getCountryName())
                    .orElseThrow(() -> new IllegalArgumentException("Unable to find country name : " + refogUser.getCountryName())));
        }
        dbUser.setUsername(refogUser.getUsername());
        userRepository.save(dbUser);
        modifiedUsers.add(userRepository.findByUID(dbUser.getUsername()));
    }


    private void updatingModifiedFieldsInDb(User dbUser, RefogUser refogUser) {
        if (!StringUtils.equalsIgnoreCase(dbUser.getFirstName(), refogUser.getFirstName())) {
            dbUser.setFirstName(refogUser.getFirstName());
        }
        if (!StringUtils.equalsIgnoreCase(dbUser.getLastName(), refogUser.getLastName())) {
            dbUser.setLastName(refogUser.getLastName());
        }
        if (!StringUtils.equalsIgnoreCase(dbUser.getEmail(), refogUser.getEmail())) {
            dbUser.setEmail(refogUser.getEmail());
        }
        dbUser.setUsername(refogUser.getUsername());
        userRepository.save(dbUser);
        modifiedUsers.add(userRepository.findByUID(dbUser.getUsername()));
    }

    private boolean compareAllFieldsOfTwoObjects(User a, RefogUser b) {
        return StringUtils.equalsIgnoreCase(a.getUsername(), b.getUsername()) &&
                StringUtils.equalsIgnoreCase(a.getBusinessLine(), b.getBusinessLine()) &&
                StringUtils.equalsIgnoreCase(a.getEntity(), b.getEntity()) &&
                StringUtils.equalsIgnoreCase(a.getPole(), b.getPole()) &&
                StringUtils.equalsIgnoreCase(getCountryName(a.getCountry()), b.getCountryName()) &&
                StringUtils.equalsIgnoreCase(a.getFirstName(), b.getFirstName()) &&
                StringUtils.equalsIgnoreCase(a.getLastName(), b.getLastName()) &&
                StringUtils.equalsIgnoreCase(a.getEmail(), b.getEmail());
    }

    private void usersToBeModifyMapping(User dbUser) {
        User updatingUser = new User();
        updatingUser.setId(dbUser.getId());
        updatingUser.setUsername(dbUser.getUsername());
        updatingUser.setCountry(dbUser.getCountry());
        updatingUser.setPole(dbUser.getPole());
        updatingUser.setEntity(dbUser.getEntity());
        updatingUser.setBusinessLine(dbUser.getBusinessLine());
        updatingUser.setFirstName(dbUser.getFirstName());
        updatingUser.setLastName(dbUser.getLastName());
        updatingUser.setEmail(dbUser.getEmail());
        updatingUser.setProfile(dbUser.getProfile());
        updatingUser.setLastConnection(dbUser.getLastConnection());
        updatingUser.setAssignedListPermissions(dbUser.getAssignedListPermissions());
        updatingUser.setDateOfUpdate(dbUser.getDateOfUpdate());
        updatingUser.setExportedFileDownloadPermissionEndDate(dbUser.getExportedFileDownloadPermissionEndDateOpt().orElse(null));
        updatingUser.setImportedFileDownloadPermissionEndDate(dbUser.getImportedFileDownloadPermissionEndDateOpt().orElse(null));
        updatingUser.setStatus(dbUser.getStatus());
        usersToBeModified.add(updatingUser);
    }

    private void compareAndSetEmptyRefogFields(RefogUser refogUser) {
        if (StringUtils.isEmpty(refogUser.getBusinessLine())) {
            refogUser.setBusinessLine(null);
        }
        if (StringUtils.isEmpty(refogUser.getEntity())) {
            refogUser.setEntity(null);
        }
        if (StringUtils.isEmpty(refogUser.getPole())) {
            refogUser.setPole(null);
        }
        if (StringUtils.isEmpty(refogUser.getEmail())) {
            refogUser.setEmail(null);
        }
        if (StringUtils.isEmpty(refogUser.getCountryName())) {
            refogUser.setCountryName(null);
        }
        if (StringUtils.isEmpty(refogUser.getFirstName())) {
            refogUser.setFirstName(null);
        }
        if (StringUtils.isEmpty(refogUser.getLastName())) {
            refogUser.setLastName(null);
        }
    }

    private UserDifferenceMapper updatingExcelSheetForDeactivatedUsers(UserDifferenceMapper updateExcel, User user) {
        updateExcel.setUid(user.getUsername());
        updateExcel.setFirstNamePreviousValueInRegliss(user.getFirstName());
        updateExcel.setFirstNameNewValueInRegliss("");
        updateExcel.setLastNamePreviousValueInRegliss(user.getLastName());
        updateExcel.setLastNameNewValueInRegliss("");
        if (user.getProfile() != null)
            updateExcel.setProfile(user.getProfile().name());
        else updateExcel.setProfile(null);
        if (user.getStatus() != null)
            updateExcel.setPostBatchStatus(user.getStatus().name());
        else updateExcel.setPostBatchStatus(null);
        if (user.getLastConnection() != null)
            updateExcel.setLastConnection(user.getLastConnection().toString());
        else updateExcel.setLastConnection(null);
        if (user.getCountry() != null)
            updateExcel.setCountryPreviousValueinRegliss(getCountryName(user.getCountry()));
        else updateExcel.setCountryPreviousValueinRegliss(null);
        updateExcel.setCountryNewValueinRegliss("");
        updateExcel.setBusinesLinePreviousValueinRegliss(user.getBusinessLine());
        updateExcel.setBusinessLineNewValueinRegliss("");
        updateExcel.setPolePreviousValueinRegliss(user.getPole());
        updateExcel.setPoleNewValueinRegliss("");
        updateExcel.setEntityPreviousValueinRegliss(user.getEntity());
        updateExcel.setEntityNewValueinRegliss("");
        updateExcel.setEmailPreviousValueinRegliss(user.getEmail());
        updateExcel.setEmailNewValueinRegliss("");
        return updateExcel;
    }

    private void deactivatingDetectedUsers(List<User> goToInactivateUsers) {
        for (User user : goToInactivateUsers) {
            User deactivatingUser = usersMapping(user);
            deactivatingUser.setStatus(User.Status.INACTIVE);
            userRepository.save(deactivatingUser);
            deactivatedUsers.add(userRepository.findByUID(deactivatingUser.getUsername()));
        }
        log.info("Deactivated users in DB : {} ", deactivatedUsers.size());
    }

    private void usersToBeDeactivatedMapping(User user) {
        User mappingUser = new User();
        mappingUser.setId(user.getId());
        mappingUser.setUsername(user.getUsername());
        mappingUser.setCountry(user.getCountry());
        mappingUser.setPole(user.getPole());
        mappingUser.setEntity(user.getEntity());
        mappingUser.setBusinessLine(user.getBusinessLine());
        mappingUser.setFirstName(user.getFirstName());
        mappingUser.setLastName(user.getLastName());
        mappingUser.setEmail(user.getEmail());
        mappingUser.setProfile(user.getProfile());
        mappingUser.setLastConnection(user.getLastConnection());
        mappingUser.setAssignedListPermissions(user.getAssignedListPermissions());
        mappingUser.setDateOfUpdate(user.getDateOfUpdate());
        mappingUser.setExportedFileDownloadPermissionEndDate(user.getExportedFileDownloadPermissionEndDateOpt().orElse(null));
        mappingUser.setImportedFileDownloadPermissionEndDate(user.getImportedFileDownloadPermissionEndDateOpt().orElse(null));
        mappingUser.setStatus(User.Status.ACTIVE);
        usersToBeDeactivated.add(mappingUser);
    }

    private User usersMapping(User user) {
        User mappingUser = new User();
        mappingUser.setId(user.getId());
        mappingUser.setUsername(user.getUsername());
        mappingUser.setCountry(user.getCountry());
        mappingUser.setPole(user.getPole());
        mappingUser.setEntity(user.getEntity());
        mappingUser.setBusinessLine(user.getBusinessLine());
        mappingUser.setFirstName(user.getFirstName());
        mappingUser.setLastName(user.getLastName());
        mappingUser.setEmail(user.getEmail());
        mappingUser.setProfile(user.getProfile());
        mappingUser.setLastConnection(user.getLastConnection());
        mappingUser.setAssignedListPermissions(user.getAssignedListPermissions());
        mappingUser.setDateOfUpdate(user.getDateOfUpdate());
        mappingUser.setExportedFileDownloadPermissionEndDate(user.getExportedFileDownloadPermissionEndDateOpt().orElse(null));
        mappingUser.setImportedFileDownloadPermissionEndDate(user.getImportedFileDownloadPermissionEndDateOpt().orElse(null));
        mappingUser.setStatus(user.getStatus());
        return mappingUser;
    }

    private List<User> findUsersNeedstoBeDeactivated(List<User> activeAndNonIgnoredUsers, List<RefogUser> refogUsers) {
        List<User> notAvailableUsers = new ArrayList<>();
        List<User> inActiveUsersForLongDays = new ArrayList<>();

        Set<String> refogUsernames = refogUsers.stream()
                .map(RefogUser::getUsername)
                .collect(Collectors.toSet());

        for (User user : activeAndNonIgnoredUsers) {
            if (!refogUsernames.contains(user.getUsername())) {
                notAvailableUsers.add(user);
            }

            LocalDateTime lastConnection = user.getLastConnection();
            if (lastConnection != null) {
                long days = Duration.between(lastConnection, LocalDateTime.now()).toDays();
                if (days > inActiveDays) {
                    inActiveUsersForLongDays.add(user);
                }
            } else {
                inActiveUsersForLongDays.add(user);
            }
        }
        List<User> goToInactivateUsers = Stream.concat(notAvailableUsers.stream(), inActiveUsersForLongDays.stream()).distinct().collect(Collectors.toList());

        for (User user : goToInactivateUsers) {
            usersToBeDeactivatedMapping(user);
        }
        log.info("Users needs to be deactived : {} , Not available users in refog : {} , Inactive users for long period in regliss: {} ", usersToBeDeactivated.size(),
                notAvailableUsers.size(), inActiveUsersForLongDays.size());
        return goToInactivateUsers;
    }

    private List<User> findActiveAndNonIgnoredUsers(List<User> allUsersInRepo) {
        List<User> activeAndNonIgnoredUsers = new ArrayList<>();
        for (User user : allUsersInRepo) {
            if (user.getStatus() == User.Status.ACTIVE && (!userIgnoreProfiles.contains(user.getUsername()))) {
                activeAndNonIgnoredUsers.add(user);
            }
        }
        return activeAndNonIgnoredUsers.stream().distinct().collect(Collectors.toList());
    }

    private String getCountryName(Country dbcountry) {
        if (dbcountry != null && dbcountry.getId() != null && dbcountry.getId() != 0) {
            Optional<Country> country = countryRepo.findById(dbcountry.getId());
            if (country.isPresent()) {
                return country.get().getLabel();
            } else
                return null;
        }
        return null;
    }
}
