package com.bnpp.regliss.facade.dto.reference;

public class ProfileReferentialDto extends EnumReferentialDto{

    public boolean downloadImportAssignable;
    public boolean downloadExportAssignable;

    public ProfileReferentialDto() { super(); }
    public ProfileReferentialDto(boolean downloadImportAssignable, boolean downloadExportAssignable, String id, String label) {
        super(id, label);
        this.downloadImportAssignable = downloadImportAssignable;
        this.downloadExportAssignable = downloadExportAssignable;
    }
    public boolean isDownloadImportAssignable() { return downloadImportAssignable; }
    public boolean isDownloadExportAssignable() { return downloadExportAssignable; }

}

