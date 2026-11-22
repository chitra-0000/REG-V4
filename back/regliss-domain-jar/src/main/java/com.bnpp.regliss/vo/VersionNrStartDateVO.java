package com.bnpp.regliss.vo;

import java.time.LocalDate;

import lombok.Data;

@Data
public class VersionNrStartDateVO {
    private final Long versionNumber;
    private final LocalDate startDate;

}
