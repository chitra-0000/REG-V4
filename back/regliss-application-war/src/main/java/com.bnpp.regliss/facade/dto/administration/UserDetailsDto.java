package com.bnpp.regliss.facade.dto.administration;

// [MISSING FROM SCREENSHOTS: lines 3-11 of the original file were not visible in the photos]

public class UserDetailsDto {

    public Long id;
    public String username; // UID
    public String firstName;
    public String lastName;
    public User.Status status;
    public Language language;
    public Long country;
    public String pole;
    public String entity;
    public String businessLine;
    public String email;
    public boolean userDownloadExportVersionPermissionAssignable;
    public boolean hasExportDownloadPermission;
    public LocalDate exportDownloadPermissionEndDate;
    public boolean userDownloadImportVersionPermissionAssignable;
    public boolean hasImportDownloadPermission;
    public LocalDate importDownloadPermissionEndDate;
    public Profile profile;
    public String allowedListsShortnames;
    public LocalDateTime lastConnection;
    public LocalDateTime dateOfUpdate;
}

