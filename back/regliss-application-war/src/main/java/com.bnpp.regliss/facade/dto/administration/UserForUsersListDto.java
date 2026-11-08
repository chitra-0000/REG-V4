package com.bnpp.regliss.facade.dto.administration;

// [MISSING FROM SCREENSHOTS: lines 3-8 of the original file were not visible in the photos]

public class UserForUsersListDto {
    public Long id;
    public String username; // UID
    public String firstName;
    public String lastName;
    public Profile profile;
    public Long country;
    public boolean hasExportDownloadPermission;
    public LocalDate exportDownloadPermissionEndDate;
    public boolean hasImportDownloadPermission;
    public LocalDate importDownloadPermissionEndDate;
    public Status status;
    public List<String> allowedListsShortnames;
    public LocalDateTime lastConnection;
    public LocalDateTime dateOfUpdate;
    private boolean hasAssignedLists;

    public boolean isHasAssignedLists() { return hasAssignedLists; }

    public void setHasAssignedLists(boolean hasAssignedLists) { this.hasAssignedLists = hasAssignedLists; }
}

