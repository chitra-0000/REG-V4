package com.bnpp.regliss.service.permission;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.Scope;
import com.bnpp.regliss.search.ReglissListSearchResult;
import com.bnpp.regliss.vo.ListBreadcrumbVO;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class ListPermissionRequest {
    private final Long id;
    private final Confidentiality confidentiality;
    private final Scope scope;
    private final Long countryId;

    public ListPermissionRequest(Long id, Confidentiality confidentiality, Scope scope, Long countryId) {
        this.id = id;
        this.confidentiality = confidentiality;
        this.scope = scope;
        this.countryId = countryId;
    }

    public ListPermissionRequest(ReglissList list) {
        this(list.getId(), list.getConfidentiality(), list.getScope(), list.getCountry().getId());
    }

    public ListPermissionRequest(ListBreadcrumbVO list) {
        this(list.getId(), list.getConfidentiality(), list.getScope(), list.getCountry().getId());
    }

    public ListPermissionRequest(ReglissListSearchResult result) {
        this(result.getId(), result.getConfidentiality(), result.getScope(), result.getCountry());
    }

    public Long getId() {
        return id;
    }

    public Confidentiality getConfidentiality() {
        return confidentiality;
    }

    public Scope getScope() {
        return scope;
    }

    public Long getCountryId() {
        return countryId;
    }
}
