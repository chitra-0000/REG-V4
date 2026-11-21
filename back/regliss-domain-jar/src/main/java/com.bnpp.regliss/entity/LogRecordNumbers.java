package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class LogRecordNumbers {

    @Getter
    @EqualityField
    @Column(name = "TOTAL_RECORD_NUMBER")
    private Long total;

    @Getter
    @EqualityField
    @Column(name = "ADDED_RECORD_NUMBER")
    private Long add;

    @Getter
    @EqualityField
    @Column(name = "MOD_RECORD_NUMBER")
    private Long mod;

    @Getter
    @EqualityField
    @Column(name = "SUP_RECORD_NUMBER")
    private Long sup;

    @Getter
    @EqualityField
    @Column(name = "TOTAL_ENTITIES_NUMBER")
    private Long totalEntities;

    @Getter
    @EqualityField
    @Column(name = "TOTAL_PERSONS_NUMBER")
    private Long totalPersons;

    @Getter
    @EqualityField
    @Column(name = "ADDED_ENTITIES_NUMBER")
    private Long addedEntities;

    @Getter
    @EqualityField
    @Column(name = "ADDED_PERSONS_NUMBER")
    private Long addedPersons;

    @Getter
    @EqualityField
    @Column(name = "MOD_ENTITIES_NUMBER")
    private Long modEntities;

    @Getter
    @EqualityField
    @Column(name = "MOD_PERSONS_NUMBER")
    private Long modPersons;

    @Getter
    @EqualityField
    @Column(name = "SUP_ENTITIES_NUMBER")
    private Long subEntities;

    @Getter
    @EqualityField
    @Column(name = "SUP_PERSONS_NUMBER")
    private Long subPersons;

    @EqualityField
    @Getter
    @Column(name = "UPLOAD_DATE")
    private String uploadDate;

    public LogRecordNumbers() {}

    public LogRecordNumbers(long total, long add, long mod, long sup, long addedEntities,long addedPersons,long modEntities, long modPersons,long subEntities,long subPersons,String uploadDate) {
        this.total = total;
        this.add = add;
        this.mod = mod;
        this.sup = sup;
        this.addedEntities=addedEntities;
        this.addedPersons=addedPersons;
        this.subEntities=subEntities;
        this.subPersons=subPersons;
        this.modEntities=modEntities;
        this.modPersons=modPersons;
        this.uploadDate=uploadDate;
    }

    public LogRecordNumbers setTotal(Long total) {
        this.total = total;
        return this;
    }

    public LogRecordNumbers setAdd(Long add) {
        this.add = add;
        return this;
    }

    public LogRecordNumbers setMod(Long mod) {
        this.mod = mod;
        return this;
    }

    public LogRecordNumbers setSup(Long sup) {
        this.sup = sup;
        return this;
    }

    public LogRecordNumbers setRecordDetailedNumbers(long totalEntities, long totalPersons, long addedEntities, long addedPersons) {
        this.totalEntities = totalEntities;
        this.totalPersons = totalPersons;
        this.addedEntities = addedEntities;
        this.addedPersons = addedPersons;
        return this;
    }
}
