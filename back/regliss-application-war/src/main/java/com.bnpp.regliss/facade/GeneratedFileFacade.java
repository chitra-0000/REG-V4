package com.bnpp.regliss.facade;

import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.ExportFile.GenerationStatus;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.exporter.BatchExportService;
import com.bnpp.regliss.exporter.GeneratedFileService;
import com.bnpp.regliss.facade.dto.version.FileToRegenerateDto;
import com.bnpp.regliss.facade.dto.version.GeneratedFileDto;
import com.bnpp.regliss.facade.mapper.VersionMapper;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.ConcatenatedListRepository;
import com.bnpp.regliss.repository.GeneratedFileRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.service.GenerationReason;
import com.bnpp.regliss.service.SubscriberService;
import com.bnpp.regliss.service.permission.CurrentVersionPermissions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Facade
@ReglissWebProfile
@Slf4j
public class GeneratedFileFacade {

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private GeneratedFileRepository repo;

    @Autowired
    private GeneratedFileService service;

    @Autowired
    private CurrentVersionPermissions versionPermissions ;

    @Autowired
    SubscriberService subscriberService;

    @Autowired
    private BatchExportService batchExportService;

    @Autowired
    private VersionMapper versionMapper;

    @Autowired
    private ConcatenatedListRepository concatenatedlistRepo;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void regenerateVersionFormats(Long versionId, List<FileExportFormat> formats) {
        List<Long> generatedFiles = service.persistVersionGeneratedFileStubsInTx(versionId, formats);
        batchExportService.persistVersionExportRegenerationInTx(versionId, generatedFiles);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void regenerateVersionFormatsAndWLFilters(Long versionId, List<FileToRegenerateDto> formats) {
        List<Long> generatedFiles = service.persistGeneratedFileAndWLFiltersStubsInTx(versionId, formats);
        batchExportService.persistVersionExportRegenerationInTx(versionId, generatedFiles);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void regenerateConcatenatedExportFiles(Long id, List<FileExportFormat> formats) {
        List<Long> generatedFileIds = service.persistConcatenatedListGeneratedFilesStubsInTx(id, formats);
        batchExportService.persistConcatenatedListExportGenerationInTx(generatedFileIds, GenerationReason.REGENERATION);
    }

    public List<FileExportFormat> getConcatenatedExportFormatsToRegenerate(Long id){
        ConcatenatedList concatenatedList = concatenatedlistRepo.getExactlyOne(id);
        List<FileExportFormat> currentExportFormats = concatenatedList.getConcatenatedListExportFormats();
        List<GeneratedFile> latestGeneratedFiles = service.getLatestGeneratedFiles(concatenatedList.getGeneratedFiles()).stream()
                .filter(generatedFile -> currentExportFormats.contains(generatedFile.getFileExportFormat()))
                .collect(toList());
        return this.getFinalFormatsToRegenerate(currentExportFormats, latestGeneratedFiles);
    }

    private List<FileExportFormat> getFinalFormatsToRegenerate(List<FileExportFormat> currentExportFormats,
                                                               List<GeneratedFile> generatedFiles){
        List<FileExportFormat> formatsToRegenerate = new ArrayList<>();
        Set<FileExportFormat> generatedExportFormats = generatedFiles.stream()
                .map(GeneratedFile::getFileExportFormat)
                .collect(Collectors.toSet());

        if(currentExportFormats.size() == generatedExportFormats.size()){
            formatsToRegenerate.addAll(generatedFiles.stream()
                    .filter(gf->gf.getGenerationStatus() == GenerationStatus.KO || gf.getGenerationStatus() == GenerationStatus.IN_PROGRESS)
                    .map(GeneratedFile::getFileExportFormat)
                    .collect(Collectors.toSet()));
        } else {
            formatsToRegenerate.addAll(currentExportFormats.stream()
                    .filter(format -> !generatedExportFormats.contains(format))
                    .collect(Collectors.toList()));
            formatsToRegenerate.addAll(generatedFiles.stream()
                    .filter(gf->gf.getGenerationStatus() == GenerationStatus.KO || gf.getGenerationStatus() == GenerationStatus.IN_PROGRESS)
                    .map(GeneratedFile::getFileExportFormat)
                    .collect(Collectors.toSet()));
        }
        return formatsToRegenerate;
    }

    public List<GeneratedFileDto> getLatestGeneratedFilesForVersion(Long versionId) {
        versionPermissions.checkConsultVersionAccess(versionId);
        return service.getLatestGeneratedFilesForVersion(versionId).stream()
                .map(GeneratedFileDto::new)
                .collect(toList());
    }

    public List<GeneratedFileDto> getLatestGeneratedFilesForOfac() {
        return service.getLatestGeneratedFilesForOfac().stream()
                .map(GeneratedFileDto::new)
                .collect(toList());
    }

    public List<GeneratedFileDto> getLatestGeneratedFilesForConcatenatedList(Long id) {
        ConcatenatedList concatenatedList = concatenatedlistRepo.getExactlyOne(id);
        List<FileExportFormat> exportFormatsForConcatenatedList = concatenatedList.getConcatenatedListExportFormats();
        return service.getLatestGeneratedFiles(concatenatedList.getGeneratedFiles()).stream()
                .filter(generatedFile -> exportFormatsForConcatenatedList.contains(generatedFile.getFileExportFormat()))
                .map(GeneratedFileDto::new)
                .collect(toList());

    }

    public GeneratedFileDto getGeneratedFile(Long id) { return new GeneratedFileDto(repo.getExactlyOne(id)); }

    public Set<FileExportFormat> getFormatsThatCanBeRegenerated(Long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        List<GeneratedFileFormat> allAvailableFormats = subscriberService.generateAllCombinationsForList(version.getList());
        List<GeneratedFile> latestGeneratedFiles = service.getLatestGeneratedFilesForVersion(versionId);
        return allAvailableFormats.stream()
                .filter(fileFormat -> formatCanBeRegenerated(fileFormat, latestGeneratedFiles, version))
                .map(GeneratedFileFormat::getFileFormat)
                .collect(toSet());
    }

    public Set<FileToRegenerateDto> getFormatsAndWLFiltersThatCanBeRegenerated(Long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        List<GeneratedFileFormat> allAvailableFormats = subscriberService.generateAllCombinationsForList(version.getList());
        List<GeneratedFile> latestGeneratedFiles = service.getLatestGeneratedFilesForVersion(versionId);
        if(version.getList().getActiveAndNotDeletedFilters().isEmpty()) {
            Set<String> formats = allAvailableFormats.stream()
                    .filter(fileFormat -> formatCanBeRegenerated(fileFormat, latestGeneratedFiles, version))
                    .map(f->f.getFileFormat().toString())
                    .collect(toSet());
            Set<FileToRegenerateDto> set=new HashSet<>();
            for(String format:formats){
                FileToRegenerateDto formatDto=new FileToRegenerateDto();
                formatDto.formatOrFilterName=format;
                set.add(formatDto);
            }
            return set;
        }else {
            Set<DJExportFilter> filters=new HashSet<>();
            for(DJExportFilter f:version.getList().getActiveAndNotDeletedFilters()){
                filters.add(f);
            }
            return filters.stream()
                    .filter(DJExportFilter::isGenerateExportFiles)
                    .map(f->versionMapper.toFilterToRegenerateDto(f))
                    .collect(toSet());
        }
    }

    private boolean formatCanBeRegenerated(GeneratedFileFormat fileFormat, List<GeneratedFile> latestFiles, Version version) {
        if(!sourceDataIsAvailable(version, fileFormat)){
            log.info("Source data is not available");
            return false;
        }
        List<GeneratedFile> latestFilesForFormat = latestFiles.stream()
                .filter(versionFile -> versionFile.getGeneratedFileFormat().equals(fileFormat))
                .collect(toList());
        if (latestFilesForFormat.isEmpty()) { //formats added after previous generation
            return true;
        }

        return (latestFilesForFormat.stream().anyMatch(this:: fileCanBeRegenerated));
    }

    private boolean fileCanBeRegenerated( GeneratedFile generatedFile) {
        if (generatedFile.getGenerationStatus() == GenerationStatus.OK) {
            return  generatedFile.getFileContent() == null;//content is purged

        } else {
            return true; // IN_PROGRESS or KO
        }
    }

    private boolean sourceDataIsAvailable(Version version, GeneratedFileFormat fileFormat) {
        if (fileFormat.getFileFormat().isDJFormat()) {
            VersionDJ versionDJ = version.getVersionDJ();
            boolean djSourceDataWasPurged = versionDJ.getReferentialsXmlPrefixSnippet() == null;
            if (djSourceDataWasPurged) {
                return false;
            }
        }
        return true;
    }
}

