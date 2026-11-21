package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.facade.*;
import com.bnpp.regliss.facade.dto.SingleValue;
import com.bnpp.regliss.facade.dto.VersionBreadcrumbDto;
import com.bnpp.regliss.facade.dto.record.ProgramDto;
import com.bnpp.regliss.facade.dto.record.RecordVerificationInfoDto;
import com.bnpp.regliss.facade.dto.version.*;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.repository.pagination.PaginationOptions;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.service.permission.CurrentVersionPermissions;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.vo.ImportLineErrorReport;
import com.bnpp.regliss.web.filter.SecuredFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("api/versions")
@ReglissWebProfile
public class VersionController {

    @Autowired
    private VersionFacade versionFacade;

    @Autowired
    private GeneratedFileFacade generatedFileFacade;

    @Autowired
    private FileDeliveryFacade fileDeliveryFacade;

    @Autowired
    private ManualImportFacade importFacade;

    @Autowired
    private CurrentVersionPermissions versionPermissions;

    @Autowired
    private AddModSupFacade addModSupFacade;

    @Autowired
    private ReglissRequestContext requestContext;

    @SecuredFeature(FeaturePermission.versionCreate)
    @PostMapping
    public SingleValue createVersion(@RequestParam("idList") Long listId){
        versionPermissions.checkCanCreateVersion(listId);
        long newId = versionFacade.createManualVersion(listId);
        return new SingleValue(newId);
    }

    @SecuredFeature({FeaturePermission.versionViewBeforeCertification, FeaturePermission.versionViewCertified, FeaturePermission.versionViewAfterPublication})
    @GetMapping("/{id}")
    public VersionDtoView getVersion(@PathVariable Long id){
        versionPermissions.checkConsultVersionAccess(id);
        return versionFacade.getVersion(id);
    }

    @SecuredFeature({FeaturePermission.versionViewBeforeCertification, FeaturePermission.versionViewCertified, FeaturePermission.versionViewAfterPublication})
    @GetMapping("/{id}/synthesis")
    public VersionSynthesisDto getVersionForSynthesisActionButtons(@PathVariable Long id) {
        versionPermissions.checkConsultVersionAccess(id);
        return versionFacade.getVersionForSynthesis(id);
    }

    @SecuredFeature({FeaturePermission.versionViewBeforeCertification, FeaturePermission.versionViewCertified, FeaturePermission.versionViewAfterPublication})
    @GetMapping("/breadcrumb-for-version/{id}")
    public VersionBreadcrumbDto getBreadcrumbForVersion(@PathVariable Long id){
        versionPermissions.checkConsultVersionAccess(id);
        return versionFacade.getBreadcrumbForVersion(id);
    }

    @SecuredFeature({FeaturePermission.versionEditBeforeCertification, FeaturePermission.versionEditCertified, FeaturePermission.versionEditConfirmed,
            FeaturePermission.versionEditAfterCertification})
    @PutMapping
    public void updateVersion(@RequestBody VersionDtoForm dto){
        if (!versionPermissions.getActions(dto.id).save) {
            throw new ReglissException(ErrorCode.LOST_LOCK_FOR_SAVE_VERSION);
        }
        versionPermissions.checkModifyVersionAccess(dto.id);
        versionFacade.updateVersion(dto);
    }

    @SecuredFeature({FeaturePermission.versionEditBeforeCertification, FeaturePermission.versionEditCertified, FeaturePermission.versionEditConfirmed,
            FeaturePermission.versionEditAfterCertification})
    @PutMapping("/{id}/update-record-in-synthesis")
    public void bulkUpdateOfVersionStatusAndComment(@PathVariable Long id, @RequestBody List<RecordVerificationInfoDto> dtoList) {
        if (!versionPermissions.getActions(id).save) {
            throw new ReglissException(ErrorCode.LOST_LOCK_FOR_SAVE_VERSION);
        }
        versionPermissions.checkModifyVersionAccess(id);
        dtoList.forEach(dto -> {
            versionFacade.updateVersionInSynthesis(id, dto);
        });
    }

    @SecuredFeature(FeaturePermission.versionDelete)
    @DeleteMapping("/{id}")
    public void deleteVersion(@PathVariable long id){
        if (!versionPermissions.getActions(id).delete) {
            throw new ReglissException(ErrorCode.NOT_AUTHORIZED);
        }
        versionFacade.deleteVersion(id);
    }

    @SecuredFeature(FeaturePermission.versionValidate)
    @PutMapping("/{id}/validate")
    public void validateVersion(@PathVariable long id){
        if (!versionPermissions.getActions(id).validate) {
            throw new ReglissException(ErrorCode.NOT_AUTHORIZED);
        }
        versionFacade.validateVersion(id);
    }

    @SecuredFeature(FeaturePermission.versionValidate)
    @PutMapping("/{id}/return")
    public void returnVersion(@PathVariable long id, @RequestBody SingleValue messageDto){
        if (!versionPermissions.getActions(id).returnValidation) {
            throw new ReglissException(ErrorCode.NOT_AUTHORIZED);
        }
        versionFacade.returnVersion(id, (String)messageDto.value);
    }

    @SecuredFeature({FeaturePermission.versionCertify, FeaturePermission.versionConfirm})
    @PutMapping("/{id}/certify")
    public void certifyOrConfirmVersion(@PathVariable long id, @RequestBody SingleValue confirmationMessage){
        if (!versionPermissions.getActions(id).certify &&
            !versionPermissions.getActions(id).confirm) {
            throw new ReglissException(ErrorCode.NOT_AUTHORIZED);
        }
        versionFacade.certifyOrConfirmVersion(id, (String) confirmationMessage.value);
    }

    @GetMapping("/{id}/programs-for-synthesis")
    public List<ProgramDto> getProgramsForSynthesis(@PathVariable Long id) {
        versionPermissions.checkConsultVersionAccess(id);
        return versionFacade.getProgramsForSynthesis(id);
    }

    @GetMapping("/{id}/number-of-records-for-synthesis")
    public SingleValue getNumberOfRecordsForSynthesis(@PathVariable Long id) {
        versionPermissions.checkConsultVersionAccess(id);
        return new SingleValue(versionFacade.getNumberOfRecordsForSynthesis(id));
    }

    @PostMapping("/{id}/created-records")
    public PaginatedResults<VersionSynthesisRowDto> getCreatedRecordsForVersion(
            @PathVariable Long id, @RequestBody PaginationOptions pagination){
        versionPermissions.checkConsultVersionAccess(id);
        return versionFacade.getCreatedRecordsForVersion(id, pagination);
    }

    @PostMapping("/{id}/updated-records")
    public PaginatedResults<VersionSynthesisRowDto> getUpdatedRecordsForVersion(
            @PathVariable Long id, @RequestBody PaginationOptions pagination){
        versionPermissions.checkConsultVersionAccess(id);
        return versionFacade.getUpdatedRecordsForVersion(id, pagination);
    }

    @PostMapping("/{id}/deleted-records")
    public PaginatedResults<VersionSynthesisRowDto> getDeletedRecordsForVersion(
            @PathVariable Long id, @RequestBody PaginationOptions pagination){
        versionPermissions.checkConsultVersionAccess(id);
        return versionFacade.getDeletedRecordsForVersion(id, pagination);
    }

    @SecuredFeature(FeaturePermission.versionReglementationDownload)
    @GetMapping ("/{id}/programs-for-reglementary")
    public List<ProgramDto> getProgramsForReglementary(@PathVariable Long id) {
        versionPermissions.checkConsultVersionAccess(id);
        return versionFacade.getProgramsForReglementary(id);
    }

    @SecuredFeature(FeaturePermission.versionEditBeforeCertification)
    @PostMapping("/{id}/upload-manual-import-file")
    public UploadedFileDto uploadManualImportFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException{
        versionPermissions.checkConsultVersionAccess(id);
        return importFacade.uploadManualImportFile(id, file.getOriginalFilename(), file.getInputStream());
    }

    @GetMapping("/{id}/import-status")
    public UploadedFileDto importStatus(@PathVariable Long id) {
        return importFacade.getManualImportStatus(id);
    }

    @GetMapping("/{id}/generated-files")
    public List<GeneratedFileDto> getGeneratedFiles(@PathVariable Long id) {
        versionPermissions.checkConsultVersionAccess(id);
        return generatedFileFacade.getLatestGeneratedFilesForVersion(id);
    }

    @SecuredFeature(FeaturePermission.versionRegenerate)
    @GetMapping("/{id}/formats-to-regenerate")
    public Set<FileExportFormat> getFormatsToRegenerate(@PathVariable Long id) {
        versionPermissions.checkConsultVersionAccess(id);
        return generatedFileFacade.getFormatsThatCanBeRegenerated(id);
    }

    @SecuredFeature(FeaturePermission.versionRegenerate)
    @GetMapping("/{id}/formats-wlfilters-to-regenerate")
    public Set<FileToRegenerateDto> getFormatsAndWLFiltersToRegenerate(@PathVariable Long id) {
        versionPermissions.checkConsultVersionAccess(id);
        return generatedFileFacade.getFormatsAndWLFiltersThatCanBeRegenerated(id);
    }

    @SecuredFeature(FeaturePermission.versionRegenerate)
    @PostMapping("/{id}/regenerate-formats")
    public void regenerateFormats(@PathVariable Long id, @RequestBody FileExportFormat[] formats) {
        versionPermissions.checkConsultVersionAccess(id);
        generatedFileFacade.regenerateVersionFormats(id, Arrays.asList(formats));
    }

    @SecuredFeature(FeaturePermission.versionRegenerate)
    @PostMapping("/regenerate-formats-wlfilters")
    public void regenerateFormatsAndWLFilters(@PathVariable Long id, @RequestBody FileToRegenerateDto[] formats) {
        versionPermissions.checkConsultVersionAccess(id);
        generatedFileFacade.regenerateVersionFormatsAndWLFilters(id, Arrays.asList(formats));
    }

    @SecuredFeature(FeaturePermission.versionRegenerateAddModSup)
    @GetMapping("/{versionId}/regenerate-add-mod-sup")
    public void regenerateAddModSup(@PathVariable Long versionId) {
        versionPermissions.checkConsultVersionAccess(versionId);
        addModSupFacade.regenerateAddModSup(versionId);
    }

    @SecuredFeature(FeaturePermission.versionRebroadcast)
    @PostMapping("/{id}/redeliver-files")
    public void redeliverFilesForVersion(@PathVariable Long id, @RequestBody Long[] subscriberIds) {
        versionPermissions.checkConsultVersionAccess(id);
        fileDeliveryFacade.redeliverFilesForVersion(id, Arrays.asList(subscriberIds));
    }

    @GetMapping("/{id}/get-lock-for-record-creation")
    public void getLockForRecordCreation(@PathVariable Long id) {
        if (!versionPermissions.getActions(id).editRecords) {
            throw new ReglissException(ErrorCode.NOT_AUTHORIZED);
        }
        versionFacade.getLockForRecordCreation(id);
    }

    @GetMapping("/{id}/unlock")
    public void unlockVersion(@PathVariable Long id) {
        versionFacade.unlock(id);
    }

    @SecuredFeature(FeaturePermission.unlockVersionManually)
    @GetMapping("/{id}/unlock-locked-by-any-user")
    public void unlockVersionLockedByAnyUser(@PathVariable Long id) {
        versionFacade.unlockLockedByAnyUser(id);
    }

    @SecuredFeature(FeaturePermission.versionEditBeforeCertification)
    @PostMapping("/{id}/revert")
    public void revertVersion(@PathVariable Long id) {
        if (!versionPermissions.getActions(id).upload) {
            throw new ReglissException(ErrorCode.NOT_AUTHORIZED);
        }
        versionFacade.revert(id);
    }

    @SecuredFeature({FeaturePermission.versionVerify, FeaturePermission.versionEditAfterCertification})
    @PutMapping("/{id}/verify")
    public void verifyVersion(@PathVariable Long id, @RequestBody SingleValue verificationMessage) {
        if (!versionPermissions.getActions(id).verify) {
            throw new ReglissException(ErrorCode.NOT_AUTHORIZED);
        }
        versionFacade.verifyVersion(id, (String) verificationMessage.value);
        addModSupFacade.regenerateAddModSupAuditTrial(id);
    }

    @GetMapping("/{id}/semiauto-import-errors")
    public List<ImportLineErrorReport> getSemiAutoImportErrors(@PathVariable Long id){
        versionPermissions.checkConsultVersionAccess(id);
        return versionFacade.getSemiAutoImportErrors(id);
    }

    @SecuredFeature({FeaturePermission.versionEditBeforeCertification, FeaturePermission.versionEditCertified, FeaturePermission.versionEditConfirmed,
            FeaturePermission.versionEditAfterCertification})
    @PutMapping("/{id}/temporaryChange")
    public void updateTemporaryChanges(@PathVariable Long id, @RequestBody RecordVerificationInfoDto verifiedRecord){
        if (!versionPermissions.getActions(id).save) {
            throw new ReglissException(ErrorCode.LOST_LOCK_FOR_SAVE_VERSION);
        }
        versionPermissions.checkModifyVersionAccess(id);
        versionFacade.saveOrUpdateTemporaryChange(id, verifiedRecord);
    }

    @PostMapping("/{id}/cancel")
    public void cancelVersion(@PathVariable Long id) {
        if(!requestContext.isLowAuthentication()) {
            versionPermissions.checkModifyVersionAccess(id);
        }
        versionFacade.deleteTemporaryVerificationCommentsStatusVersion(id);
    }

    @SecuredFeature(FeaturePermission.userDownloadVerificationCommentsAllowed)
    @GetMapping("/{id}/regenerate-add-mod-sup-audit-trial")
    public void regenerateAddModSupAuditTrial(@PathVariable Long id) {
        versionPermissions.checkConsultVersionAccess(id);
        addModSupFacade.regenerateAddModSupAuditTrial(id);
    }
}

