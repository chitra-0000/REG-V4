package com.bnpp.regliss.utils;

import com.bnpp.regliss.vo.TruncateStringFieldsVO;
import lombok.Data;

import java.util.List;

@Data
public class TruncatedRecordFieldsValues {

    private final String className;
    private String recordExternalReference;
    private List<TruncateStringFieldsVO> truncateStringList;

    public TruncatedRecordFieldsValues(String className, List<TruncateStringFieldsVO> truncateStringList) {
        this.className = className;
        this.truncateStringList = truncateStringList;
    }

}
