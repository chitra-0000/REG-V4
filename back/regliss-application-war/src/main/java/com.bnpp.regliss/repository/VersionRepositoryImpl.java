package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.Version;

import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

public class VersionRepositoryImpl extends BaseRepositoryImpl<Version> implements VersionRepositoryCustom {

    public String refreshSearchRecordView(Integer hist) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("REFRESH_MV_REGLISS");
        query.registerStoredProcedureParameter("p_hist", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_out", String.class, ParameterMode.OUT);
        query.setParameter("p_hist", hist);
        query.execute();
        return query.getOutputParameterValue("p_out").toString();
    }

}

