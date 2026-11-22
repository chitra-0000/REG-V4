package com.bnpp.regliss.service.permission;

import static java.util.Arrays.asList;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bnpp.regliss.ReglissEnvironment;
import com.bnpp.regliss.entity.Profile;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.vo.FeaturePermission;

@Service
public class CurrentFeaturePermissions {

    @Autowired
    private ReglissRequestContext requestContext;

    public Set<FeaturePermission> getFeaturePermissions() {
        Profile profile = requestContext.getCurrentUser().getProfile();

        Set<FeaturePermission> permissions = new HashSet<>();

        if (ReglissEnvironment.isLocalDev()) {
            permissions.addAll(asList(FeaturePermission.values()));
        }
        permissions.addAll(profile.getBaseFeatures());
        if (requestContext.isHighAuthentication()) {
            permissions.add(FeaturePermission.isAuthenticationHigh);
        } else {
            permissions.removeAll(FeaturePermission.featurePermissionsRequiringHighAuth());
        }
        if (getUserDownloadExportFilesPermission()) {
            permissions.add(FeaturePermission.userDownloadExportFilesAllowed);
        }
        if (getUserDownloadImportFilesPermission()) {
            permissions.add(FeaturePermission.userDownloadImportFilesAllowed);
        }
        return permissions;
    }

    private boolean getUserDownloadExportFilesPermission() {
        Optional<LocalDate> exportDownloadEndDate = requestContext.getCurrentUser().getExportedFileDownloadPermissionEndDateOpt();
        return requestContext.isHighAuthentication() &&
                exportDownloadEndDate.isPresent() &&
                exportDownloadEndDate.get().isAfter(LocalDate.now());
    }

    private boolean getUserDownloadImportFilesPermission() {
        Optional<LocalDate> importDownloadEndDate = requestContext.getCurrentUser().getImportedFileDownloadPermissionEndDateOpt();
        return requestContext.isHighAuthentication() &&
                importDownloadEndDate.isPresent() &&
                importDownloadEndDate.get().isAfter(LocalDate.now());
    }

    public boolean has(FeaturePermission feature) {
        return getFeaturePermissions().contains(feature);
    }

}
