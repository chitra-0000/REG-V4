package com.bnpp.regliss.service.permission;

import com.bnpp.regliss.ReglissEnvironment;
import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.repository.ImportedFileRepository;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.repository.ReglementationRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.service.VersionLockService;
import com.bnpp.regliss.service.VersionService;
import com.bnpp.regliss.vo.FeaturePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class CurrentVersionPermissions {

    @Autowired
    private CurrentFeaturePermissions featurePermissions;

    @Autowired
    private VersionLockService lockService;

    @Autowired
    private VersionService versionService;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private ImportedFileRepository importedFileRepository;

    @Autowired
    private CurrentListPermissions listPermissions;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ReglementationRepository reglementationRepo;

    public void checkConsultVersionAccess(long versionId) {
        final VersionPermissionRequest versionPermissionRequest = versionRepo.getVersionPermissionRequest(versionId);
        if (versionPermissionRequest!=null) {
            checkConsultVersionAccess(versionPermissionRequest);
        }else{
            throw new ReglissException(ErrorCode.NOT_FOUND);
        }
    }
    public void checkConsultVersionAccess(VersionPermissionRequest permissionRequest) {
        boolean isConfidential = permissionRequest.getConfidentiality() == Confidentiality.CONFIDENTIAL;
        if (!requestContext.isHighAuthentication() && isConfidential) {
            throw new ReglissException(ErrorCode.AUTHENTICATION_HIGH_LEVEL_REQUIRED);
        }
        checkVersionAccessForViewRecords(permissionRequest);
    }
    public void checkModifyVersionAccess(long versionId) {
        final Version version = versionRepo.getExactlyOne(versionId);
        final VersionPermissionRequest versionPermissionRequest = new VersionPermissionRequest(version);

        checkConsultVersionAccess(versionPermissionRequest); //CHECK CONSULT ACCESS

        if (!canEditVersion(version)) {
            throw new ReglissException(ErrorCode.NOT_AUTHORIZED);
        }
    }

    public void checkDeleteRecordPermission(long versionId) {
        VersionPermissionRequest permissionRequest = versionRepo.getVersionPermissionRequest(versionId);
        checkConsultVersionAccess(permissionRequest);
        ListPermission permission = listPermissions.getCurrentPermissionForList(permissionRequest);
        if (permission != ListPermission.WRITE) {
            throw new ReglissException(ErrorCode.NO_PERMISSION_FOR_LIST);
        }
    }


    public void checkVersionAccessForViewRecords(VersionPermissionRequest version) {
        if (!isProfileAllowedToConsultVersion(version)) {
            throw new ReglissException(ErrorCode.NOT_ALLOWED_BY_PROFILE);
        }
        ListPermission permission = listPermissions.getCurrentPermissionForList(version);
        if (permission == null) {
            throw new ReglissException(ErrorCode.NO_PERMISSION_FOR_LIST);
        }
    }

    public boolean isVersionReadOnlyForDisplayInList(VersionPermissionRequest version) {
        boolean isConfidential = version.getConfidentiality() == Confidentiality.CONFIDENTIAL;
        if (!requestContext.isHighAuthentication() && isConfidential) {
            return true;
        }

        if (!isProfileAllowedToConsultVersion(version)) {
            return true;
        }
        ListPermission listPermission = listPermissions.getCurrentPermissionForList(version);
        if (listPermission == null) {
            return true;
        }
        return listPermission == ListPermission.READ;
    }


    private boolean isProfileAllowedToConsultVersion(VersionPermissionRequest version) {
        Set<FeaturePermission> features = featurePermissions.getFeaturePermissions();

        // Consult a version before confirmation/certification - Consulter une version avant Confirmation/Certification
        if (version.wasNotCertified() && features.contains(FeaturePermission.versionViewBeforeCertification)) {
            return true;
        }
        // Consult a confirmed/certified version - Consulter une version Confirmée/Certifiée
        if (version.wasCertified() && features.contains(FeaturePermission.versionViewCertified)
                && version.hasConfirmedOrCertifiedStatus()) {
            return true;
        }
        // Consult version after certification - Consulter une version après Confirmation/Certification
        return version.wasPublished() && features.contains(FeaturePermission.versionViewAfterPublication);
    }

    public VersionActions getActions(Long versionId) {
        return getActions(versionRepo.getExactlyOne(versionId));
    }

    public VersionActions getActions(Version version) {
        VersionActions buttons = new VersionActions();

        buttons.addModSupDownload = addModSupDownload(version.getId());
        buttons.addModSupAuditTrailDownload = addModSupAuditTrailDownload(version);

        if (userHasPermissionOnVersion(version)) {
            buttons.delete = version.wasNotCertified() && featurePermissions.has(FeaturePermission.versionDelete);
            buttons.save = save(version);
            buttons.validate = validate(version);
            buttons.returnValidation = returnValidation(version);
            buttons.confirm = confirm(version);
            buttons.certify = certify(version);
            buttons.verify = verify(version);
            buttons.upload = featurePermissions.has(FeaturePermission.versionEditBeforeCertification);
            buttons.editRecords = editRecords(version);
            buttons.addModSupRegenerate = addModSupRegenerate(version.getId());
            buttons.addModSupAuditTrailRegenerate = addModSupAuditTrailRegenerate(version);
        }
        return buttons;
    }

    private boolean addModSupDownload(Long versionId){
        return featurePermissions.has(FeaturePermission.userDownloadExportFilesAllowed)
                && versionService.versionIsEligibleForAddModSupExportFile(versionId);
    }

    private boolean addModSupAuditTrailDownload (Version version){
        return featurePermissions.has(FeaturePermission.userDownloadVerificationCommentsAllowed) &&
                versionService.versionIsEligibleForAddModSupAuditTrailFile(version.getId()) ;
    }

    private boolean save(Version version){
        return ( version.getStatus() != VersionStatus.VALIDATED ||
                (!version.wasAutoUploaded() && !version.isVerified()) ) &&
                canEditVersion(version);
    }

    private boolean validate(Version version){
        return !version.wasAutoUploaded()
                && featurePermissions.has(FeaturePermission.versionValidate)
                && ( validateForManualAndNotSuppoterdFormat(version) || validateForCustomManual(version));
    }
    private boolean returnValidation(Version version){
        return !version.wasAutoUploaded()
                && featurePermissions.has(FeaturePermission.versionValidate)
                && version.getStatus() == VersionStatus.VALIDATED;
    }

    private boolean confirm(Version version){
        return version.wasAutoUploaded() && featurePermissions.has(FeaturePermission.versionConfirm)
                && version.getStatus() == VersionStatus.VALIDATED;
    }
    private boolean certify(Version version){
        Long validationUserId = version.getValidationUserOpt().map(User::getId).orElse(null);
        boolean currentUserIsNotTheValidator = !requestContext.getUserId().equals(validationUserId)
                || ReglissEnvironment.isLocalDev();

        return !version.wasAutoUploaded() && currentUserIsNotTheValidator
                && featurePermissions.has(FeaturePermission.versionCertify)
                && featurePermissions.has(FeaturePermission.versionEditBeforeCertification)
                && version.getStatus() == VersionStatus.VALIDATED;
    }

    private boolean verify(Version version){
        return featurePermissions.has(FeaturePermission.versionVerify)
                && ( isVerifyAllowedFofValidatedVersions(version) ||
                (version.wasCertified() && featurePermissions.has(FeaturePermission.versionEditAfterCertification)) )
                && !version.isVerified();
    }
    private boolean editRecords(Version version){
        return featurePermissions.has(FeaturePermission.recordUpdate)
                && version.statusIsOneOf(VersionStatus.DRAFT, VersionStatus.RETURNED)
                && (version.getImportFileType().isManualProcessingFormat() ||
                version.getImportFileType().isNotSupportedFormat());
    }

    private boolean addModSupRegenerate(Long versionId){
        return featurePermissions.has(FeaturePermission.versionRegenerateAddModSup)
                && versionService.versionIsEligibleForAddModSupExportFile(versionId);
    }
    private boolean addModSupAuditTrailRegenerate(Version version){
        return featurePermissions.has(FeaturePermission.userDownloadVerificationCommentsAllowed) &&
                versionService.versionIsEligibleForAddModSupAuditTrailFile(version.getId()) ;
    }

    private boolean isVerifyAllowedFofValidatedVersions(Version version){
        return !version.wasAutoUploaded() &&
                version.wasValidated() &&
                version.getStatus() != VersionStatus.RETURNED  &&
                featurePermissions.has(FeaturePermission.versionEditBeforeCertification);
    }

    private boolean userHasPermissionOnVersion(Version version) {
        if (lockService.isLockedByAnotherUser(version.getId())) {
            log.debug("No permissions on Version: locked by another user");
            return false;
        }

        if (!listPermissions.canModifyVersion(new ListPermissionRequest(version.getList()))) {
            log.debug("No permissions on Version: no WRITE permission on List");
            return false;
        }
        return true;
    }

    public void checkCanCreateVersion(long listId) {
        listPermissions.checkCanModifyVersion(listId);
    }

    public boolean isVersionReadOnly(Version version) {
        return listPermissions.isVersionReadOnly(new VersionPermissionRequest(version)) || !canEditVersion(version);
    }

    private boolean canEditVersion(Version version) {
        ListPermission currentPermissionForList = listPermissions.getCurrentPermissionForList(new VersionPermissionRequest(version));
        if (currentPermissionForList != ListPermission.WRITE) {
            return false;
        }

        Set<FeaturePermission> currentFeaturePermissions = this.featurePermissions.getFeaturePermissions();
        // Modify a version before confirmation/certification - Modifier une version avant Confirmation/Certification
        if (version.wasNotCertified()) {
            return currentFeaturePermissions.contains(FeaturePermission.versionEditBeforeCertification);
        }
        // Modify a confirmed(auto lists) version - Modifier une version Confirmée
        if (version.isConfirmedForDJLists()) {
            return currentFeaturePermissions.contains(FeaturePermission.versionEditConfirmed);
        }
        // Modify a certified(non-auto lists) version - Modifier une version Certifiée
        if (version.isCertifiedForCustomAndNonAutoLists()) {
            return currentFeaturePermissions.contains(FeaturePermission.versionEditCertified);
        }
        // Modify version after certification - Modifier une version après Confirmation/Certification
        if (version.wasPublished()) {
            return currentFeaturePermissions.contains(FeaturePermission.versionEditAfterCertification);
        }
        return false;
    }

    public void checkReglementationAccess(Long reglementationId, Long versionId) {
        Reglementation reglementation = reglementationRepo.getExactlyOne(reglementationId);
        Version version = reglementation.getVersions()
                .stream()
                .filter(v -> v.getId().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new ReglissException(ErrorCode.VERSION_NONEXISTENT_FOR_REGLEMENTATION));

        checkConsultVersionAccess(version.getId());
    }

    public void checkRecordAccess(long versionId, String uuid) {
        Optional<Record> recordInVersion = recordRepo.findByUUIDAndVersionId(uuid, versionId);
        if (!recordInVersion.isPresent()) {
            throw new ReglissException();
        }
        checkConsultVersionAccess(versionId);
    }

    public boolean isCombinedCorrectlycheckRecordAccessHistory(long versionId, String uuid) {
        Optional<Record> record = recordRepo.findByExternalUuid(uuid);
        if(!record.isPresent()){
            throw new ReglissException();
        }
        if (record.get().getVersionLinks().getClonedInVersionId() == null || record.get().getVersionLinks().getClonedInVersionId() == versionId) {
            return true;
        }
        checkConsultVersionAccess(versionId);
        return false;
    }

    private boolean validateForManualAndNotSuppoterdFormat(Version version){
        return (version.getImportFileType().isManualProcessingFormat() ||
                version.getImportFileType().isNotSupportedFormat())    &&
                version.statusIsOneOf(VersionStatus.DRAFT, VersionStatus.RETURNED);
    }
    private boolean validateForCustomManual(Version version){
        return version.getComputedRecordNumbers().getTotal() != 0                  &&
                version.statusIsOneOf(VersionStatus.DRAFT, VersionStatus.RETURNED) &&
                importedFileRepository.findTop1ByVersionOrderByUploadDateDesc(version).map(ImportedFile::isImportOK).orElse(false);
    }
}
