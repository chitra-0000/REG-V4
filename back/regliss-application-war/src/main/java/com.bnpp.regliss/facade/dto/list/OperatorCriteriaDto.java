package com.bnpp.regliss.facade.dto.list;

import com.bnpp.regliss.entity.referential.DJSubfilterOperator;

public class OperatorCriteriaDto {

    public DJSubfilterOperator operatorSince;
    public Long years;

    public OperatorCriteriaDto (DJSubfilterOperator operatorSince, Long years){
        this.operatorSince = operatorSince;
        this.years = years;
    }
    public OperatorCriteriaDto(){}
}

