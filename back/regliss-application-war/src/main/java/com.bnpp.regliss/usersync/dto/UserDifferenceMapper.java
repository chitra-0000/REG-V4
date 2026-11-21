package com.bnpp.regliss.usersync.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class UserDifferenceMapper {

    @Getter
    @Setter
    private String uid;
    @Getter
    @Setter
    private String firstNamePreviousValueInRegliss;
    @Getter
    @Setter
    private String firstNameNewValueInRegliss;
    @Getter
    @Setter
    private String lastNamePreviousValueInRegliss;
    @Getter
    @Setter
    private String lastNameNewValueInRegliss;
    @Getter
    @Setter
    private String profile;
    @Getter
    @Setter
    private String postBatchStatus;
    @Getter
    @Setter
    private String lastConnection;
    @Getter
    @Setter
    private String countryPreviousValueinRegliss;
    @Getter
    @Setter
    private String countryNewValueinRegliss;
    @Getter
    @Setter
    private String polePreviousValueinRegliss;
    @Getter
    @Setter
    private String poleNewValueinRegliss;
    @Getter
    @Setter
    private String entityPreviousValueinRegliss;
    @Getter
    @Setter
    private String entityNewValueinRegliss;
    @Getter
    @Setter
    private String businesLinePreviousValueinRegliss;
    @Getter
    @Setter
    private String businessLineNewValueinRegliss;
    @Getter
    @Setter
    private String batchUpdateDate;
    @Getter
    @Setter
    private String emailPreviousValueinRegliss;
    @Getter
    @Setter
    private String emailNewValueinRegliss;

}

