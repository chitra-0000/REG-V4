package com.bnpp.regliss.facade.dto.list;

// [MISSING FROM SCREENSHOTS: lines 3-5 of the original file were not visible in the photos]


@Getter

@NoArgsConstructor
public class ReglissListPermissionDetailsDto {
    public Long listId;
    public Confidentiality confidentiality;
    public String listName;
    public boolean isListDeleted;
    public boolean readPermission;
    public boolean writePermission;
    public boolean downloadImportPermission;
    public boolean downloadExportPermission;
    private boolean readPermissionDisabled;
    private boolean writePermissionDisabled;

    public ReglissListPermissionDetailsDto(Long listId, Confidentiality confidentiality, String listName) {
        this.listId = listId;
        this.confidentiality = confidentiality;
        this.listName = listName;
    }

    public ReglissListPermissionDetailsDto(ReglissListPermissionDetailsDto other){
        this.listId = other.listId;
        this.confidentiality = other.confidentiality;
        this.listName = other.listName;
        this.readPermission = other.readPermission;
        this.writePermission = other.writePermission;
        this.downloadImportPermission = other.downloadImportPermission;
        this.downloadExportPermission = other.downloadExportPermission;
        this.readPermissionDisabled = other.readPermissionDisabled;
        this.writePermissionDisabled = other.writePermissionDisabled;
    }


    public ReglissListPermissionDetailsDto merge(ReglissListPermissionDetailsDto other){
        this.readPermission = this.readPermission || other.readPermission;
        this.writePermission = this.writePermission || other.writePermission;
        this.downloadImportPermission = this.downloadImportPermission || other.downloadImportPermission;
        this.downloadExportPermission = this.downloadExportPermission || other.downloadExportPermission;
        this.readPermissionDisabled = this.readPermissionDisabled || other.readPermissionDisabled;
        this.writePermissionDisabled = this.writePermissionDisabled || other.writePermissionDisabled;
        return this;
    }

    public boolean isReadPermissionDisabled() { return readPermissionDisabled; }

    public boolean isWritePermissionDisabled() { return writePermissionDisabled; }


    public void setWritePermissionDisabled(boolean writePermissionDisabled) {
        this.writePermissionDisabled = writePermissionDisabled;
    }

    public void setReadPermissionDisabled(boolean readPermissionDisabled) {
        this.readPermissionDisabled = readPermissionDisabled;
    }



}

