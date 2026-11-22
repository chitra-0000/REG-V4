package com.bnpp.regliss.search;

import com.bnpp.regliss.entity.referential.record.CodeType;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.repository.pagination.PaginationOptions;

import java.util.ArrayList;
import java.util.List;

public class RecordSearchVersionCriteria {

    public Long idVersion;
    public String program;
    public String name;
    public String codeStr;
    public CodeType codeType;
    public List<RecordType> recordTypes = new ArrayList<>();;
    public boolean isAlias;
    public PaginationOptions paginationOptions = new PaginationOptions(15);
}
