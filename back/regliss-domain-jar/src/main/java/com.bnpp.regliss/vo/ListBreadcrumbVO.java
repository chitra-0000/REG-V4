package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.Scope;
import com.bnpp.regliss.entity.referential.record.Country;

public interface ListBreadcrumbVO {
    public Long getId();
    public String getReference();
    public String getName();
    public String getShortName();
    public ImportFileType getImportFileType();
    public boolean isAllowManualUpload();
    public boolean isActive();
    public Confidentiality getConfidentiality();
    public Scope getScope();
    public Country getCountry();
}
