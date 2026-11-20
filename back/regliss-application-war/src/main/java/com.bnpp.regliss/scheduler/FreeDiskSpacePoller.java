package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.ReglissEnvironment;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ReglissBatchProfile
public class FreeDiskSpacePoller {
    @Value("${automatic.import.free_space.min_MB}")
    private long minFreeSpace;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AutomaticImportFileRepository fileRepo;


    @Scheduled(cron = "${task.free_disk_space.cron}")
    public void checkFreeDiskSpace() {
        if (ReglissEnvironment.isLocalDev()){
            return;
        }

        List<File> directoriesWithLowDiskSpace = fileRepo.getDirectoriesWithFreeSpaceBellowThreshold(minFreeSpace);
        if (!directoriesWithLowDiskSpace.isEmpty()) {
            emailService.sendEmailDjImportLowDiskSpace();
        }
    }
}

