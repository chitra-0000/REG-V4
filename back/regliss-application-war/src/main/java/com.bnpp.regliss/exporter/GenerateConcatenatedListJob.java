package com.bnpp.regliss.exporter;

import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter;
import com.bnpp.regliss.entity.ConcatenatedList;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.batch.BatchExport;
import com.bnpp.regliss.entity.batch.BatchExportParams;
import com.bnpp.regliss.exporter.launcher.ExportRegularFilesLauncher;
import com.bnpp.regliss.exporter.writer.FileGenerationTry;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.ConcatenatedListRepository;
import com.bnpp.regliss.repository.GeneratedFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.stream.Collectors.*;

@Service
@ReglissBatchProfile
@Slf4j
public class GenerateConcatenatedListJob {

    @Autowired
    private GeneratedFileRepository generatedFileRepo;

    @Autowired
    private ConcatenatedListRepository concatenatedlistRepo;

    @Autowired
    private GeneratedFileRepository generatedFileRepository;

    @Autowired
    private Publisher publisher;

    @Autowired
    private ExportRegularFilesLauncher exportRegularFilesLauncher;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CloseResourcesAfter
    public void generateConcatenatedListFiles(BatchExport batchExport) {
        BatchExportParams exportParams = batchExport.getExportParams();
        Set<Long> concatenatedListIds = generatedFileRepository.findAllConcatenatedListIdByGeneratedFileIds(exportParams.getGeneratedFileIds());
		
        Map<Long, List<Long>> mapConcatListReglissLists = concatenatedlistRepo.getConactListReglissListIds(concatenatedListIds).stream()
                                         .collect(toMap(arr -> (Long) arr[0], arr -> Collections.singletonList((Long) arr[1]),
                                                 GenerateConcatenatedListJob::mergeDuplicatesEntries));


        Map<GeneratedFile, FileGenerationTry> allGeneratedFiles = new HashMap<>();

        for (Map.Entry<Long, List<Long>> concatListReglissLists : mapConcatListReglissLists.entrySet()) {
            Set<GeneratedFile> filesToGenerate = generatedFileRepo.getByIdsForConcatenatedList(exportParams.getGeneratedFileIds()).stream()
                                                                      .filter(generatedFile -> generatedFile.getConcatenatedList().getId()
                                                                              .equals(concatListReglissLists.getKey()))
                                                                      .collect(toSet());
            ExportJobParameters exportJobParameters = new ExportJobParameters(filesToGenerate, false,
                    batchExport.getBatchExecutionId());
            Map<GeneratedFile, FileGenerationTry> generatedFiles = exportRegularFilesLauncher
                    .generateConcatenatedListRegularFiles(exportJobParameters, concatListReglissLists.getValue());
            allGeneratedFiles.putAll(generatedFiles);
        }

        publisher.publishFilesForConcatenatedList(allGeneratedFiles, exportParams.getReason());
    }

    private static List<Long> mergeDuplicatesEntries(List<Long> existingResults, List<Long> newResults){
        List<Long> mergeResults = new ArrayList<>();
        mergeResults.addAll(existingResults);
        mergeResults.addAll(newResults);
        return mergeResults;
    }

}
