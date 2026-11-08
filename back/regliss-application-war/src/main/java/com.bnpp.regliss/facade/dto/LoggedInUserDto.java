package com.bnpp.regliss.facade.dto;

// [MISSING FROM SCREENSHOTS: lines 3-7 of the original file were not visible in the photos]

public class LoggedInUserDto  {

    public long id;
    public String firstName;
    public String lastName;
    public String username;
    public String email;
    public String language;
    public String fullName;
    public Profile profile;
    public Map<FeaturePermission, Boolean> permissions = new EnumMap<>(FeaturePermission.class);
}

