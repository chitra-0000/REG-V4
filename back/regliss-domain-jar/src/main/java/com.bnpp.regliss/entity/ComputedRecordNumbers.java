package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ComputedRecordNumbers {

    @Getter
    @EqualityField
    @Column(name = "TOTAL_RECORD_NUMBER")
    private long total;

    @Getter
    @EqualityField
    @Column(name = "ADDED_RECORD_NUMBER")
    private long add;

    @Getter
    @EqualityField
    @Column(name = "MOD_RECORD_NUMBER")
    private long mod;

    @Getter
    @EqualityField
    @Column(name = "MOD_REC_NO_FOR_SYNTH")
    private long modForSynthesis;

    @Getter
    @EqualityField
    @Column(name = "SUP_RECORD_NUMBER")
    private long sup;

    @Getter
    @EqualityField
    @Column(name = "STRONG_ALIAS_RECORD_NUMBER")
    private long strong;

    @Getter
    @EqualityField
    @Column(name = "WEAK_ALIAS_RECORD_NUMBER")
    private long weak;

    @Getter
    @Column(name = "MOD_PERSONS_NUMBER")
    private Long modPersons;

    @Getter
    @Column(name = "SUP_PERSONS_NUMBER")
    private Long supPersons;

    @Getter
    @Column(name = "ADD_PERSONS_NUMBER")
    private Long addPersons;

    public ComputedRecordNumbers() {}

    public ComputedRecordNumbers(long total, long add, long mod, long modForSynthesis, long sup, long strong, long weak) {
        this.total = total;
        this.add = add;
        this.mod = mod;
        this.modForSynthesis=modForSynthesis;
        this.sup = sup;
        this.strong = strong;
        this.weak = weak;
    }

    public ComputedRecordNumbers setTotal(long total) {
        this.total = total;
        return this;
    }

    public ComputedRecordNumbers setAdd(long add) {
        this.add = add;
        return this;
    }

    public ComputedRecordNumbers setMod(long mod) {
        this.mod = mod;
        return this;
    }

    public ComputedRecordNumbers setModForSynthesis(long modForSynthesis) {
        this.modForSynthesis = modForSynthesis;
        return this;
    }

    public ComputedRecordNumbers setSup(long sup) {
        this.sup = sup;
        return this;
    }

    public ComputedRecordNumbers setStrong(long strong) {
        this.strong = strong;
        return this;
    }

    public ComputedRecordNumbers setWeak(long weak) {
        this.weak = weak;
        return this;
    }

    public ComputedRecordNumbers setModPersons(long modPersons) {
        this.modPersons = modPersons;
        return this;
    }

    public ComputedRecordNumbers setSupPersons(long supPersons) {
        this.supPersons = supPersons;
        return this;
    }

    public ComputedRecordNumbers setAddPersons(long addPersons) {
        this.addPersons = addPersons;
        return this;
    }
}
