package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.entity.Profile;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.profile.ReglissPublisherProfile;
import com.bnpp.regliss.repository.UserRepository;
import com.bnpp.regliss.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@Component
@Slf4j
@ReglissPublisherProfile
public class AdminEmailExpiringRightsScheduler {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private HeartbeatService heartbeatService;

    /**
     * Use this method to check expiring download rights
     */
    @Scheduled(cron = "${task.check.user.download.rights.cron}")
    public void sendMailWithUsersExpiringDownloadPermission() {
        if (heartbeatService.isNodeActivePublisher()) {
            List<String> listSupportAdmins = userRepository.findByProfile(Profile.ADMIN_SUPPORT)
                    .stream().filter(o -> o.getEmail() != null && o.isActive())
                    .map(User::getEmail).collect(toList());
            LocalDate expirationDate = LocalDateTime.now().plusDays(5).toLocalDate();
            LocalDate ignoreDate = LocalDateTime.now().toLocalDate();
            List<User> usersExpiredPermission = userRepository.findByDownloadPermissionEndDateExpiredBetween(expirationDate, ignoreDate);
            List<String> usersWithImportExpired = usersExpiredPermission
                    .stream().filter(o -> {
                        Optional<LocalDate> endDateOpt = o.getImportedFileDownloadPermissionEndDateOpt();
                        return endDateOpt.isPresent() && endDateOpt.get().isBefore(expirationDate) && o.isActive();
                    }).map(getUserIdEmailFunction()).collect(toList());
            List<String> usersWithExportExpired = usersExpiredPermission
                    .stream().filter(o -> {
                        Optional<LocalDate> endDateOpt = o.getExportedFileDownloadPermissionEndDateOpt();
                        return endDateOpt.isPresent() && endDateOpt.get().isBefore(expirationDate) && o.isActive();
                    }).map(getUserIdEmailFunction()).collect(toList());

            log.info("Mail with users whose downloading rights are expiring in 5 days, the date '{}'",
                    expirationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            emailService.sendEmailExpireDownloadRights(
                    listSupportAdmins, usersWithImportExpired, usersWithExportExpired);
        }

    }

    private Function<User, String> getUserIdEmailFunction() {
        return u -> u.getUsername() + ((u.getEmail() != null) ? ":"+u.getEmail() : "");
    }

}

