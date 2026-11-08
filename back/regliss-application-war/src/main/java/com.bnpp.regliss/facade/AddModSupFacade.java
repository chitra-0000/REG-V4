package com.bnpp.regliss.facade;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.AddModSupAuditTrailFile;
import com.bnpp.regliss.entity.AddModSupExportFile;
import com.bnpp.regliss.entity.ExportFile.GenerationStatus;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.exporter.BatchExportService;
import com.bnpp.regliss.exporter.GeneratedFileService;
import com.bnpp.regliss.facade.dto.version.AddModSupAuditTrailFileDto;
import com.bnpp.regliss.facade.dto.version.AddModSupExportFileDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.service.GenerationReason;
import com.bnpp.regliss.service.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Optional;

@Facade
@ReglissWebProfile
public class AddModSupFacade {

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private GeneratedFileService generatedFileService;

    @Autowired
    private VersionService versionService;

    @Autowired
    private BatchExportService batchExportService;

    @Value("${amount.regeneration.synthesis.minutes}")
    private long regenerationMinutes;

    public AddModSupExportFileDto getAddModSupExportFile(Long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        if(!versionService.versionIsEligibleForAddModSupExportFile(versionId)) {
            throw new ReglissException(ErrorCode.ADD_MOD_SUP_NOT_ELIGIBLE_FOR_VERSION);
        }
        Optional<AddModSupExportFile> addModSupExportFileOpt = version.getLastAddModSupExportFileOpt();
        if (!addModSupExportFileOpt.isPresent()) {
            throw new ReglissException(ErrorCode.NOT_FOUND);
        } else {
            return new AddModSupExportFileDto(addModSupExportFileOpt.get());
        }
    }

    public void regenerateAddModSup(Long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        if(!versionService.versionIsEligibleForAddModSupExportFile(versionId)) {
            throw new ReglissException(ErrorCode.ADD_MOD_SUP_NOT_ELIGIBLE_FOR_VERSION);
        }
        Optional<AddModSupExportFile> addModSupExportFileOpt = version.getLastAddModSupExportFileOpt();
        if (addModSupExportFileOpt.isPresent()) {
            //meantime the file was generated
            AddModSupExportFile addModSupExportFile = addModSupExportFileOpt.get();
            if (addModSupExportFile.getGenerationStatus() == GenerationStatus.OK) {
                throw new ReglissException(ErrorCode.ADD_MOD_SUP_ALREADY_GENERATED);
            }
            if (addModSupExportFile.getGenerationStatus() == GenerationStatus.IN_PROGRESS) {
                LocalDateTime newRegenerationTime = addModSupExportFile.getGenerationDate().plusMinutes(regenerationMinutes);
                if(LocalDateTime.now().isBefore(newRegenerationTime)) {
                    //there is a minimum amount of time between two regenerations
                    throw new ReglissException(ErrorCode.MIN_AMOUNT_BETWEEN_REGENERATION);
                }
            }
        }

        AddModSupExportFile generatedFile = generatedFileService.createAddModSupFileStub(version);
        batchExportService.persistAddModSupExportGenerationInTx(version, generatedFile.getId(), GenerationReason.REGENERATION);

    }

    public AddModSupAuditTrailFileDto getAddModSupAuditTrailFile(Long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        if(!versionService.versionIsEligibleForAddModSupAuditTrailFile(versionId)) {
            throw new ReglissException(ErrorCode.ADD_MOD_SUP_NOT_ELIGIBLE_FOR_VERSION);
        }
        Optional<AddModSupAuditTrailFile> addModSupAuditTrailFileOpt = version.getLastOKAddModSupAuditTrailFileOpt();
        if (!addModSupAuditTrailFileOpt.isPresent()) {
            throw new ReglissException(ErrorCode.NOT_FOUND);
        } else {
            return new AddModSupAuditTrailFileDto(addModSupAuditTrailFileOpt.get());
        }
    }

    public void regenerateAddModSupAuditTrial(Long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        if(!versionService.versionIsEligibleForAddModSupAuditTrailFile(versionId)) {
            throw new ReglissException(ErrorCode.ADD_MOD_SUP_NOT_ELIGIBLE_FOR_VERSION);
        }
        Optional<AddModSupAuditTrailFile> addModSupAuditTrailFileOpt = version.getLastAddModSupAuditTrailFileOpt();
        if (addModSupAuditTrailFileOpt.isPresent()) {
            AddModSupAuditTrailFile  addModSupAuditTrailFile = addModSupAuditTrailFileOpt.get();
            if (addModSupAuditTrailFile.getGenerationStatus() == GenerationStatus.IN_PROGRESS) {
                LocalDateTime newRegenerationTime = addModSupAuditTrailFile.getGenerationDate().plusMinutes(regenerationMinutes);
                if(LocalDateTime.now().isBefore(newRegenerationTime)) {
                    //there is a minimum amount of time between two regenerations
                    throw new ReglissException(ErrorCode.MIN_AMOUNT_BETWEEN_REGENERATION);
                }
            }
        }

        AddModSupAuditTrailFile generatedFile = generatedFileService.createAddModSupAuditTrailFileStub(version);
        batchExportService.persistAddModSupAuditTrailGenerationInTx(version, generatedFile.getId(), GenerationReason.REGENERATION);
    }
}
