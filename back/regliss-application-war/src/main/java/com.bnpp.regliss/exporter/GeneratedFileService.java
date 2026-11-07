package com.bnpp.regliss.exporter;

import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.bnpp.regliss.facade.dto.version.FileToRegenerateDto;
import org.apache.commons.codec.digest.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import com.bnpp.regliss.*;
import com.bnpp.regliss.ReglissException.*;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.ExportFile.*;
import com.bnpp.regliss.entity.referential.*;
import com.bnpp.regliss.exporter.writer.*;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.service.*;
import com.bnpp.regliss.utils.*;

import lombok.extern.slf4j.*;

@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class GeneratedFileService implements IGeneratedFileService {

    @Autowired
    private GeneratedFileRepository repo;

    @Autowired
    private ConcatenatedListRepository concatenatedlistRepo;

    @Autowired
    private AddModSupExportFileRepository addModSupExportFileRepository;

    @Autowired
    private AddModSupAuditTrailFileRepository addModSupAuditTrailFileRepository;

    @Autowired
    private SubscriberService subscriberService;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private GeneratedFileNameService nameService;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private BlobService blobService;

    @Autowired
    private IAutocloseableResources fileHelper;

    @Override
    public List<GeneratedFile> createGeneratedFileStubsAfterCertification(Version version) {
        log.info("Persisting files to writeRecords for version {}", version.getId());
        List<GeneratedFile> generatedFiles = new ArrayList<>();
        generatedFiles.addAll(createGeneratedFilesForVersion(version, subscriberService.generateAllCombinationsForList(version.getList())));
        generatedFiles.addAll(createDeliveryReportFiles(version));
        repo.saveAll(generatedFiles);
        return generatedFiles;
    }

    @Override
    public AddModSupExportFile createAddModSupFileStub(Version version) {
        log.info("Persisting ADD MOD SUP file stub for version {}", version.getId());
        AddModSupExportFile addModSupExportFile = new AddModSupExportFile();
        addModSupExportFile.setFileName(nameService.getAddModSupExcelFilename(version));
        addModSupExportFile.setGenerationStatus(GenerationStatus.IN_PROGRESS);
        addModSupExportFile.setGenerationDate(requestContext.getRequestTime());
        version.addAddModSupExportFile(addModSupExportFile);
        addModSupExportFileRepository.save(addModSupExportFile);
        return addModSupExportFile;
    }

    @Override
    public AddModSupAuditTrailFile createAddModSupAuditTrailFileStub(Version version) {
        log.info("Persisting ADD MOD SUP AUDIT TRAIL file stub for version {}", version.getId());
        AddModSupAuditTrailFile addModSupAuditTrailFile = new AddModSupAuditTrailFile();
        addModSupAuditTrailFile.setFileName(nameService.getAddModSupAuditTrailFilename(version));
        addModSupAuditTrailFile.setGenerationStatus(GenerationStatus.IN_PROGRESS);
        addModSupAuditTrailFile.setGenerationDate(LocalDateTime.now());
        addModSupAuditTrailFile.setDeprecated(false);
        version.addAddModSupAuditTrailFile(addModSupAuditTrailFile);
        addModSupAuditTrailFileRepository.save(addModSupAuditTrailFile);
        return addModSupAuditTrailFile;
    }

    private List<GeneratedFile> createDeliveryReportFiles(Version version) {
        return version.getList().getActiveSubscriptions().stream()
                .filter(Subscription::hasDeliveryReport)
                .map(Subscription::getSubscriber)
                .distinct()
                .map(subscriber -> createAndPersistDeliveryReportFileForVersionAndSubscriber(version, subscriber))
                .collect(toList());
    }

    public List<GeneratedFile> createGeneratedFilesForVersion(Version version, List<GeneratedFileFormat> formats) {
        return formats.stream()
                .filter(format -> format.getFileFormat() != FileExportFormat.DELIVERY_REPORT)
                .map(format -> createGeneratedFileStub(format, version, null))
                .collect(toList());
    }

    @Override
    @Transactional(propagation =Propagation.REQUIRES_NEW)
    public List<Long> persistConcatenatedListGeneratedFilesStubsInTx(Long id, List<FileExportFormat> formats) {
        List<GeneratedFile> generatedFiles = new ArrayList<>();
        Version concatenatedListVersion = null;
        ConcatenatedList concatenatedList = concatenatedlistRepo.getExactlyOne(id);
        for (GeneratedFileFormat format : subscriberService.generateConcatenatedListCombinations(concatenatedList, formats)) {
            generatedFiles.add(repo.save(createGeneratedFileStub(format, concatenatedListVersion, concatenatedList)));
        }
        concatenatedList.setPublicationDate(requestContext.getRequestTime());
        return generatedFiles.stream().map(GeneratedFile::getId).collect(toList());
    }

    /* (non-Javadoc)
     * @see com.bnpp.regliss.exporter.IGeneratedFileService#persistOfacGeneratedFilesStubs()
     */
    @Override
    public List<GeneratedFile> persistOfacGeneratedFilesStubs() {
        List<GeneratedFile> generatedFiles = new ArrayList<>();
        for (GeneratedFileFormat format : subscriberService.generateOfacCombinations(Arrays.asList(FileExportFormat.values()))) {
            Version ofacVersion = null;
            generatedFiles.add(repo.save(createGeneratedFileStub(format, ofacVersion, null)));
        }
        return generatedFiles;
    }

    @Override
    public List<GeneratedFile> getLatestGeneratedFilesForVersion(Long versionId) {
        return getLatestGeneratedFiles(repo.getVersionGeneratedFiles(versionId)).stream()
                .filter(file->file.getGeneratedFileFormat().getFilter()==null||file.getGeneratedFileFormat().getFilter().getExtensions()
                        .contains(file.getGeneratedFileFormat().getFileExtension())).collect(toList());
    }

    public List<GeneratedFile> getLatestGeneratedFilesForOfac() {
        return getLatestGeneratedFiles(repo.getOfacGeneratedFiles());
    }

    public List<GeneratedFile> getLatestGeneratedFiles(List<GeneratedFile> generatedFiles) {
        Map<GeneratedFileFormat, Optional<GeneratedFile>> generatedFilesByFormat = generatedFiles.stream()
                .collect(groupingBy(GeneratedFile::getGeneratedFileFormat, maxBy(comparing(GeneratedFile::getGenerationDate))));
        return generatedFilesByFormat.values().stream()
                .map(Optional::get)
                .sorted(comparing(GeneratedFile::getGenerationDate).reversed())
                .collect(toList());
    }

    private GeneratedFile createGeneratedFileStub(GeneratedFileFormat format, Version version, ConcatenatedList concatenatedList) {
        GeneratedFile generatedFile = new GeneratedFile();
        generatedFile.setGeneratedFileFormat(format);
        generatedFile.setFileName(nameService.getFilename(format, version, concatenatedList));
        generatedFile.setGenerationStatus(GenerationStatus.IN_PROGRESS);
        generatedFile.setGenerationDate(requestContext.getRequestTime());
        if (version != null) {
            version.addGeneratedFile(generatedFile);
        }
        if (concatenatedList != null) {
            concatenatedList.addGeneratedFile(generatedFile);
        }
        return generatedFile;
    }

    public GeneratedFile createAndPersistDeliveryReportFileForVersionAndSubscriber(Version version, Subscriber subscriber) {
        GeneratedFile generatedFile = new GeneratedFile();
        generatedFile.setGeneratedFileFormat(new GeneratedFileFormat(FileExportFormat.DELIVERY_REPORT, FileExportMode.FULL,
                FileExportExtension.xml));
        generatedFile.setFileName("regliss");
        generatedFile.setGenerationStatus(GenerationStatus.IN_PROGRESS);
        generatedFile.setGenerationDate(requestContext.getRequestTime());
        if (version != null) {
            version.addGeneratedFile(generatedFile);
        }
        generatedFile.setSubscriber(subscriber);
        return repo.save(generatedFile);
    }


    public Optional<GeneratedFile> getNewPersistedPVGeneratedFileStubInTx(Long subscriberId) {
        Optional<GeneratedFile> pvToGenerateOpt;
        GeneratedFile lastPV = repo.getLastGeneratedFileBySubscriberId(subscriberId);
        if (lastPV != null && !lastPV.isInProgress()) {
            Version version = lastPV.getVersionOpt().orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
            Subscriber subscriber = lastPV.getSubscriberOpt().orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
            GeneratedFile newPV = createAndPersistDeliveryReportFileForVersionAndSubscriber(version, subscriber);
            pvToGenerateOpt = Optional.of(newPV);
        } else {
            pvToGenerateOpt = Optional.empty();
        }
        return pvToGenerateOpt;
    }

    @Transactional(propagation= Propagation.REQUIRES_NEW)
    public List<Long> persistVersionGeneratedFileStubsInTx(Long versionId, List<FileExportFormat> formats) {
        Version version = versionRepo.getExactlyOne(versionId);
        List<GeneratedFileFormat> combinations = subscriberService.generateCombinationsForFormats(formats,
                version.getList().getActiveFiltersWithGenerteExportFiles());
        List<GeneratedFile> generatedFiles = createGeneratedFilesForVersion(version, combinations);
        repo.saveAll(generatedFiles);
        return generatedFiles.stream().map(AbstractSimpleEntity::getId).collect(toList());
    }

    @Transactional(propagation= Propagation.REQUIRES_NEW)
    public List<Long> persistVersionGeneratedFileAndWLFiltersStubsInTx(Long versionId, List<FileToRegenerateDto> formats) {
        Version version = versionRepo.getExactlyOne(versionId);
        List<GeneratedFileFormat> combinations;
        List<String> formatsAsStrings=new ArrayList<>();
        for(FileToRegenerateDto format:formats){
            formatsAsStrings.add(format.formatOrFilterName);
        }
        if(version.getList().getActiveAndNotDeletedFilters().isEmpty()) {
            combinations = subscriberService.generateCombinationsForRegularFormats(formatsAsStrings);
        } else{
            combinations = subscriberService.generateCombinationsForWLFilters(formatsAsStrings,version.getList()
                    .getActiveFiltersWithGenerteExportFiles());
        }
        List<GeneratedFile> generatedFiles = createGeneratedFilesForVersion(version, combinations);
        repo.saveAll(generatedFiles);
        return generatedFiles.stream().map(AbstractSimpleEntity::getId).collect(toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void uploadGenerationResultInTx(Map<GeneratedFile, FileGenerationTry> generatedFiles) {
        log.debug("Updating generation status for {} files", generatedFiles.size());
        for (Map.Entry<GeneratedFile, FileGenerationTry> entry : generatedFiles.entrySet()) {
            GeneratedFile generatedFile = entry.getKey();
            FileGenerationTry generationTry = generatedFiles.get(generatedFile);
            generatedFile = repo.getExactlyOne(entry.getKey().getId());
            if (generationTry.isOk()) {
                generatedFile.setGenerationStatus(GenerationStatus.OK);
                if (generatedFile.getGeneratedFileFormat().getFileFormat() == FileExportFormat.FIRCOSOFT
                        && generatedFile.getGeneratedFileFormat().getFileExtension() == FileExportExtension.zip) {
                    String oldFilename = generatedFile.getFileName();
                    generatedFile.setFileName(nameService.getFircosoftZipPV(oldFilename + "_" +
                            computeChecksumOfFile(generationTry.getFile())));
                }
                if (generatedFile.getGeneratedFileFormat().getFileFormat() == FileExportFormat.DJ_WATCHLIST) {
                    String oldFilename = generatedFile.getFileName();
                    generatedFile.setFileName(nameService.getWatchlistZip(oldFilename).replace("*",
                            computeChecksumOfFile(generationTry.getFile())));
                }
                uploadFileToDatabase(generatedFile, generationTry.getFile(), generatedFile.getFullName());
            } else {
                updateGeneratedFileOnError(generatedFile, generationTry.getException());
            }
        }
    }

    @Transactional(propagation= Propagation.REQUIRES_NEW)
    public void updateGeneratedFileStatusOnErrorInTx(List<Long> generatedFileIds, Exception exception) {
        for (Long generatedFileId : generatedFileIds) {
            GeneratedFile generatedFile = repo.getExactlyOne(generatedFileId);
            if (generatedFile.getGenerationStatus() == GenerationStatus.IN_PROGRESS) {
                updateGeneratedFileOnError(generatedFile, exception);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAddModSupOnErrorInTx(Long addModSupFileId, Exception exception) {
        AddModSupExportFile addModSupFile = addModSupExportFileRepository.getExactlyOne(addModSupFileId);
        if (addModSupFile.getGenerationStatus() == GenerationStatus.IN_PROGRESS) {
            updateGeneratedFileOnError(addModSupFile, exception);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAddModSupOnSuccessInTx(AddModSupExportFile fileToGenerate, File file) {
        AddModSupExportFile addModSupFile = addModSupExportFileRepository.getExactlyOne(fileToGenerate.getId());
        addModSupFile.setGenerationStatus(GenerationStatus.OK);
        uploadFileToDatabase(addModSupFile, file, addModSupFile.getFileName());
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAddModSupAuditTrailOnErrorInTx(Long addModSupAuditTrailFileId, Exception exception) {
        AddModSupAuditTrailFile addModSupAuditTrailFile = addModSupAuditTrailFileRepository.getExactlyOne(addModSupAuditTrailFileId);
        if (addModSupAuditTrailFile.getGenerationStatus() == GenerationStatus.IN_PROGRESS) {
            updateGeneratedFileOnError(addModSupAuditTrailFile, exception);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateAddModSupAuditTrailOnSuccessInTx(AddModSupAuditTrailFile fileToGenerate, File file, Optional<Version> optionalVersion) {
        AddModSupAuditTrailFile addModSupAuditTrailFile = addModSupAuditTrailFileRepository.getExactlyOne(fileToGenerate.getId());
        addModSupAuditTrailFile.setGenerationStatus(GenerationStatus.OK);
        uploadFileToDatabase(addModSupAuditTrailFile, file, addModSupAuditTrailFile.getFileName());
        deletePreviousRecordFromAddModSupAuditTrailFile(optionalVersion, addModSupAuditTrailFile.getGenerationDate());
    }

    public void updateAddModSupAuditTrailFileAsDeprecated(Version version){
        Optional<AddModSupAuditTrailFile>  addModSupAuditTrailFile = addModSupAuditTrailFileRepository
                .findLatestNonDeprecatedByVersionId(version.getId());
        if(addModSupAuditTrailFile.isPresent() && !version.isVerified()){
            addModSupAuditTrailFile.get().setDeprecated(true);
        }
    }

    private void updateGeneratedFileOnError(ExportFile generatedFile, Exception exception) {
        generatedFile.setGenerationStatus(GenerationStatus.KO);
        generatedFile.setErrorMessage(getErrorMessageToStore(exception));
    }

    private String getErrorMessageToStore(Exception exception) {
        return exception.getMessage() != null ? exception.getMessage() : exception.toString();
    }

    private void uploadFileToDatabase(ExportFile generatedFile, File tempFile, String fileName) {
        log.info("Uploading file {} of size {}", tempFile.getName(), tempFile.length());
        generatedFile.setFileContent(blobService.getZippedBlob(tempFile, fileName));
    }

    private String computeChecksumOfFile(File tempFile) {
        try {
            InputStream is = fileHelper.openInputStreamForThisThread(tempFile);
            String md5Hex = DigestUtils.md5Hex(is);  // NOSONAR: this is the hash algorithm used by Dow Jones
            return Long.toHexString(getCRC32Checksum(md5Hex.getBytes()));
        } catch (IOException e) {
            throw new ReglissException(e, "Could not open generated file: " + tempFile);
        }
    }

    private static long getCRC32Checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    private void deletePreviousRecordFromAddModSupAuditTrailFile(Optional<Version> optionalVersion, LocalDateTime generationDate){
        if(optionalVersion.isPresent()){
            Version version = versionRepo.getExactlyOne(optionalVersion.get().getId());
            Optional<AddModSupAuditTrailFile>  previousAddModSupAuditTrailFileByGenerationDateOpt = addModSupAuditTrailFileRepository
                    .getPreviousAddModSupAuditTrailFileByGenerationDate(version.getId(), generationDate);
            previousAddModSupAuditTrailFileByGenerationDateOpt.ifPresent(previousAddModSupAuditTrailFileByGenerationDate
                    -> addModSupAuditTrailFileRepository.delete(previousAddModSupAuditTrailFileByGenerationDate));
        }
    }
}
