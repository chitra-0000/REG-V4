package com.bnpp.regliss.exporter;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.batch.BatchExport;
import com.bnpp.regliss.entity.batch.BatchExportParams;
import com.bnpp.regliss.exporter.launcher.ExportAddModSupAuditTrailLauncher;
import com.bnpp.regliss.exporter.launcher.ExportAddModSupLauncher;
import com.bnpp.regliss.exporter.launcher.ExportDJFormatLauncher;
import com.bnpp.regliss.exporter.launcher.ExportRegularFilesLauncher;
import com.bnpp.regliss.exporter.writer.FileGenerationTry;
import com.bnpp.regliss.monitoring.PerformanceMonitor;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@ReglissBatchProfile
@Slf4j
public class GenerateVersionJob {

    @Autowired
    private Publisher publisher;

    @Autowired
    private GeneratedFileService generatedFileService;

    @Autowired
    private AddModSupExportFileRepository addModSupExportFileRepository;

    @Autowired
    private AddModSupAuditTrailFileRepository addModSupAuditTrailFileRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GeneratedFileRepository generatedFileRepository;

    @Autowired
    private ExportDJFormatLauncher exportDJFormatLauncher;

    @Autowired
    private ExportRegularFilesLauncher exportRegularFormatLauncher;

    @Autowired
    private ExportAddModSupLauncher exportAddModSupLauncher;

    @Autowired
    private ExportAddModSupAuditTrailLauncher exportAddModSupAuditTrailLauncher;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private DJExportFilterRepository filterRepo;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CloseResourcesAfter
    public void generateVersionFiles(BatchExport batchExport) {
        PerformanceMonitor.clearStats();
        BatchExportParams exportParams = batchExport.getExportParams();
        try {
            Long versionId = exportParams.getVersionOpt()
                    .orElseThrow(() -> new ReglissException("Required version parameter missing in export process for job id: "
                            + batchExport.getId()))
                    .getId();

            List<GeneratedFile> filesToGenerate = generatedFileRepository.getByIdsFetchAll(batchExport.getExportParams().getGeneratedFileIds());
            Map<GeneratedFile, FileGenerationTry> generatedFiles = generateVersionFiles(filesToGenerate, versionId, batchExport);

            publisher.publishFilesForVersion(versionId, generatedFiles, exportParams.getReason());
        } catch (Exception e) {
            log.error("Setting the status of ALL generated files to ERROR for params {}", exportParams);
            generatedFileService.updateGeneratedFileStatusOnErrorInTx(exportParams.getGeneratedFileIds(), e);
            emailService.sendEmailExportError(e.getMessage(), exportParams.getVersionOpt().orElse(null));
            throw e;
        } finally {
            PerformanceMonitor.printStats();
            PerformanceMonitor.clearStats();
        }
    }

    private Map<GeneratedFile, FileGenerationTry> generateVersionFiles(List<GeneratedFile> filesToGenerate, Long versionId,
                                                                       BatchExport batchExport) {
        Map<GeneratedFile, FileGenerationTry> generatedFiles = new HashMap<>();
        Version version = versionRepo.getExactlyOne(versionId);
        Optional<Version> previousVersion = versionRepo.getPreviousVersionByNumber(version.getList().getId(), version.getVersionNumber());
        ExportJobParameters exportJobParameters = new ExportJobParameters(filesToGenerate, previousVersion.isPresent(),
                batchExport.getBatchExecutionId());
        List<DJExportFilter> allFilters = filterRepo.findByActiveTrue().stream().filter(filter->!filter.isDeleted())
                .collect(Collectors.toList());
        allFilters.forEach(DJExportFilter::getActiveSubfilters);
        generatedFiles.putAll(exportDJFormatLauncher.generateVersionDJFiles(exportJobParameters, version,allFilters));
        generatedFiles.putAll(exportRegularFormatLauncher.generateVersionRegularFiles(exportJobParameters, version));
        return generatedFiles;
    }

    @Transactional(readOnly = true)
    @CloseResourcesAfter
    public void generateAddModSupFile(BatchExport batchExport) {
        PerformanceMonitor.clearStats();
        BatchExportParams exportParams = batchExport.getExportParams();
        try {
            AddModSupExportFile fileToGenerate = addModSupExportFileRepository.getExactlyOne(exportParams.getGeneratedFileIds().get(0));
            log.debug("Generating ADD MOD SUP file");
            File file = exportAddModSupLauncher.generateAddModSupFile(fileToGenerate, batchExport.getBatchExecutionId());

            publisher.publishAddModSup(fileToGenerate, file);
        } catch (Exception e) {
            log.error("Setting the status of ADD MOD SUP file to ERROR for params {}", exportParams);
            generatedFileService.updateAddModSupOnErrorInTx(exportParams.getGeneratedFileIds().get(0), e);
            throw e;
        } finally {
            PerformanceMonitor.printStats();
            PerformanceMonitor.clearStats();
        }
    }

    @Transactional(readOnly = true)
    @CloseResourcesAfter
    public void generateAddModSupAuditTrailFile(BatchExport batchExport) {
        PerformanceMonitor.clearStats();
        BatchExportParams exportParams = batchExport.getExportParams();
        try {
            AddModSupAuditTrailFile fileToGenerate = addModSupAuditTrailFileRepository.getExactlyOne(exportParams.getGeneratedFileIds().get(0));
            log.debug("Generating ADD MOD SUP AUDIT TRAIL file");
            File file = exportAddModSupAuditTrailLauncher.generateAddModSupAuditTrailFile(fileToGenerate, batchExport.getBatchExecutionId());
            publisher.publishAddModSupAuditTrail(fileToGenerate, file, batchExport.getExportParams().getVersionOpt());
        } catch (Exception e) {
            log.error("Setting the status of ADD MOD SUP AUDIT TRAIL file to ERROR for params {}", exportParams);
            generatedFileService.updateAddModSupAuditTrailOnErrorInTx(exportParams.getGeneratedFileIds().get(0), e);
            throw e;
        } finally {
            PerformanceMonitor.printStats();
            PerformanceMonitor.clearStats();
        }
    }

}
