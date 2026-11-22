package com.bnpp.regliss.search;

import java.time.*;
import java.util.*;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.referential.record.*;
import com.bnpp.regliss.repository.pagination.*;

public class RecordSearchCriteria {

    public String name;
    public boolean isAlias;
    public List<RecordType> recordTypes = new ArrayList<>();
    public List<Long> categoryId = new ArrayList<>();
    public String program;
    public Set<Long> listIds = new HashSet<>();
    public List<ReglissListType> listTypes = new ArrayList<>();
    public Long versionNumber;
    public LocalDate activeFrom;
    public LocalDate activeTo;

    public PaginationOptions paginationOptions = new PaginationOptions(15);
    public boolean includePreviousVersions;
    public boolean authenticatedLowLevel;

    public CodeType codeType;
    public String codeStr;

}
