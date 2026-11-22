package com.bnpp.regliss.service;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;

import java.util.*;

import com.bnpp.regliss.entity.ConcatenatedList;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.repository.ConcatenatedListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.repository.VersionRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConcatenatedListService {

    @Autowired
    private IGeneratedFileService generatedFileService;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private ConcatenatedListRepository concatenatedListRepo;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private IBatchExportService batchExportService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> getConcatenatedListsFilesToGenerateInTx() {
        Map<Long, List<Version>> mapConcatListVersions = versionRepo.getAllLatestVersionsByConcatenatedList().stream()
                .collect(toMap(arr -> (Long) arr[0], arr -> Collections.singletonList((Version) arr[1]), ConcatenatedListService::mergeDuplicatesEntries));
        List<Long> concatenatedListToBeGenerated = getConcatenatedListToBeGenerated(mapConcatListVersions);
        if (!concatenatedListToBeGenerated.isEmpty()) {
            Map<Long, List<FileExportFormat>> concatenatedListIdsAndExportFormats = concatenatedListRepo.findAllById(concatenatedListToBeGenerated).stream()
                    .collect(toMap(ConcatenatedList::getId, ConcatenatedList::getConcatenatedListExportFormats, (existing, replacement) -> existing, HashMap::new));

            List<Long> totalGeneratedFileIds = new ArrayList<>();
            concatenatedListIdsAndExportFormats.entrySet().forEach(longListEntry -> {
                List<Long> generatedFileIds = generatedFileService.persistConcatenatedListGeneratedFilesStubsInTx(longListEntry.getKey(), longListEntry.getValue());
                totalGeneratedFileIds.addAll(generatedFileIds);
            });

            setConcatenatedListPublicationDate(mapConcatListVersions, concatenatedListToBeGenerated);
            return totalGeneratedFileIds;
        } else {
            log.trace("No new Concatenated list file to generate.");
            return emptyList();
        }
    }

    private static List<Version> mergeDuplicatesEntries(List<Version> existingResults, List<Version> newResults){
        List<Version> mergeResults = new ArrayList<>();
        mergeResults.addAll(existingResults);
        mergeResults.addAll(newResults);
        return mergeResults;
    }

    private List<Long> getConcatenatedListToBeGenerated(Map<Long, List<Version>> mapConcatListVersions){
        List<Long> concatenatedListsToBeGenerated = new ArrayList<>();
        for (Map.Entry<Long, List<Version>> elem : mapConcatListVersions.entrySet()) {
            if(elem.getValue().stream().anyMatch(v -> v.getConcatenatedListPublicationDate() == null))
                concatenatedListsToBeGenerated.add(elem.getKey());
        }
        return concatenatedListsToBeGenerated;
    }

    private void setConcatenatedListPublicationDate(Map<Long, List<Version>> mapConcatListVersions, List<Long> concatenatedListToBeGenerated){
        for (Map.Entry<Long, List<Version>> elem : mapConcatListVersions.entrySet()) {
            if(concatenatedListToBeGenerated.contains(elem.getKey())){
                elem.getValue().forEach(version -> {
                    version.setConcatenatedListPublicationDate(requestContext.getRequestTime());
                    log.info("Set Concatenated List publish date for version {} with status {} and publication date {}",version.getId(),version.getStatus(),version.getPublicationDate());
                });
            }
        }
    }
}
