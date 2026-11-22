package com.bnpp.regliss.vo;

import java.util.HashSet;

import static java.util.Arrays.asList;

// use this annotation to suppress warning related to the names of the constants
@SuppressWarnings("all")
public enum FeaturePermission {
    listSearch,
    listCreate,

    listEdit,
    listInputEdit,
    listBroadcastListEdit,
    listDelete,

    generateExportForConcatenatedList,
    redeliverFilesForConcatenatedList,

    versionCreate,
    versionCertify,
    versionValidate,
    versionDelete,
    versionVerify,
    versionConfirm,
    versionRegenerate,
    versionRegenerateAddModSup,
    versionRebroadcast,

    versionEditBeforeCertification,
    versionEditConfirmed,
    versionEditCertified,
    versionEditAfterCertification,
    versionViewBeforeCertification,
    versionViewCertified, //TODO does this require high authentication?

    versionViewAfterPublication,


    recordSearch,
    recordUpdate,

    programCreate,
    programEdit,

    versionReglementationEdit,
    versionReglementationDownload, //can also consult the reglementation

    listReglementationEdit,
    listReglementationDownload, //can also consult the reglementation TODO maybe change to listReglementationDownloadConsult !!! the value might be used hardcoded in other places

    searchUsers,
    manageUsers,
    manageSubscribers,

    broadcastView,

    ofacRegenerate,
    ofacRebroadcast,
    ofacRebroadcastAllSubscribers,
    ofacViewDeliveries,
    unlockVersionManually,

    substitutionMatrixEdit,
    substitutionMatrixView,

    djImportAdmin,
    userDownloadExportVersionPermissionAssignable,
    userDownloadExportVersionPermissionModify,
    userDownloadExportFilesAllowed,
    userUploadGuide,

    userDownloadImportVersionPermissionAssignable,
    userDownloadImportVersionPermissionModify,
    userDownloadImportFilesAllowed,

    userDownloadVerificationCommentsAllowed,

    manageMessages,
    wlFilterManagement,

    isAuthenticationHigh;

    public static HashSet<FeaturePermission> featurePermissionsRequiringHighAuth() {
        return new HashSet<>(asList(
                FeaturePermission.listInputEdit,
                FeaturePermission.listCreate,
                FeaturePermission.listEdit,
                FeaturePermission.listDelete,
                FeaturePermission.listBroadcastListEdit,

                FeaturePermission.versionCreate,
                FeaturePermission.versionValidate,
                FeaturePermission.versionDelete,
                FeaturePermission.versionConfirm,
                FeaturePermission.versionCertify,
                FeaturePermission.versionVerify,
                FeaturePermission.versionEditBeforeCertification,
                FeaturePermission.versionEditAfterCertification,
                FeaturePermission.versionRegenerate,
                FeaturePermission.versionRegenerateAddModSup,
                FeaturePermission.versionRebroadcast,
                FeaturePermission.recordUpdate,
                FeaturePermission.versionReglementationEdit,
                FeaturePermission.listReglementationEdit,

                FeaturePermission.searchUsers,
                FeaturePermission.manageUsers,
                FeaturePermission.manageSubscribers,
                FeaturePermission.ofacRegenerate,
                FeaturePermission.ofacRebroadcast,
                FeaturePermission.ofacViewDeliveries,
                FeaturePermission.unlockVersionManually,
                FeaturePermission.djImportAdmin,
                FeaturePermission.userDownloadExportVersionPermissionModify,
                FeaturePermission.userDownloadImportVersionPermissionModify,

                FeaturePermission.userDownloadVerificationCommentsAllowed,

                FeaturePermission.manageMessages
            ));
    }

}
