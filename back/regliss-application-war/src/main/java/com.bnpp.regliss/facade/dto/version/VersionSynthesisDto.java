package com.bnpp.regliss.facade.dto.version;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class VersionSynthesisDto {
    public Long id;
    public VersionSynthesisActions actions;
    public boolean canRegenerateAddModSupFile;
    public boolean canRegenerateAddModSupAuditTrailFile;
    public boolean canDownloadAddModSupAuditTrailFile;
    public boolean isDeprecatedAddModSupAuditTrailFile;
}

