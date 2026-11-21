package com.bnpp.regliss.entity;

import com.bnpp.regliss.entity.record.ToStringAuditable;
import com.bnpp.regliss.entity.referential.DJSubfilterOperator;
import lombok.Data;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
@Data
public class DJSubfilterDateCriteria implements ToStringAuditable{

    @Enumerated(EnumType.STRING)
    public DJSubfilterOperator operator;

    public Long years;


    public DJSubfilterDateCriteria(DJSubfilterOperator operator, Long years) {
        this.operator = operator;
        this.years = years;
    }
    public DJSubfilterDateCriteria(){}

    public DJSubfilterDateCriteria(DJSubfilterDateCriteria other) { this(other.operator, other.years); }

    @Override
    public String toStringAudit() { return "[" + operator.getValue() + years + "]"; }

}
