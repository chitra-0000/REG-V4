package com.bnpp.regliss.six.scheduler;

import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.scheduler.AutomaticFeedAggregator;
import com.bnpp.regliss.scheduler.AutomaticSixImportFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ReglissBatchProfile
@Slf4j
public class SixImportPoller {

    @Autowired
    private AutomaticSixImportFileRepository automaticSixImportFileRepository;

    @Autowired
    private AutomaticFeedAggregator automaticFeedAggregator;

    @Value("${allow.six.file.integration}")
    private String allowSixFilesToIntegrate;

    @Value("${six.file.prefix}")
    private String sixFileNamePrefix;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean processThreeFiles() {
        List<String> unlocked = automaticSixImportFileRepository.getFilesInInputFolder();
        Set<String> processSixFiles = Arrays.stream(allowSixFilesToIntegrate.toUpperCase().split(",")).collect(Collectors.toSet());

        List<String> ignoreFiles = unlocked.stream().filter(f -> f.startsWith(sixFileNamePrefix) && processSixFiles.stream().noneMatch(f::contains)).collect(Collectors.toList());
        if (!ignoreFiles.isEmpty()) {
            for (String fileName : ignoreFiles) {
                automaticSixImportFileRepository.moveToIgnoreDirectoryByFileName(fileName);
            }
        }

        List<String> sixFilesReadyToProcess = automaticFeedAggregator.checkAnySixFilesAvailableToBePrecessed(unlocked, processSixFiles);
        return sixFilesReadyToProcess.size() > 1;
    }
}

