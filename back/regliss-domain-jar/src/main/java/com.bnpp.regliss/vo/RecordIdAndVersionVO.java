package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.Version;

import lombok.Data;

@Data
public class RecordIdAndVersionVO {
    private final Long recordId;
    private final Version version;
}
