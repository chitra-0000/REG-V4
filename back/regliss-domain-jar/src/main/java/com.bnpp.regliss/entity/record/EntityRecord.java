package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.referential.record.RecordType;

import javax.persistence.*;
import javax.validation.Valid;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue(value = RecordType.DiscriminatorValues.ENTITY)
@CaseInsensitive
public class EntityRecord extends Record implements AddressContainer {

    @Valid
    @ImportableChild(type = ChildType.BIC)
    @ManyToMany(cascade = CascadeType.ALL)
    @AuditedCollection(createAction = AuditedAction.RECORD_BIC_ADDITION,
            updateAction = AuditedAction.RECORD_BIC_MODIFICATION,
            deleteAction = AuditedAction.RECORD_BIC_DELETION)
    @JoinTable(name = "RECORD_BIC", joinColumns = @JoinColumn(name = "RECORD_ID"), inverseJoinColumns = @JoinColumn(name = "BIC_ID"))
    private Set<CodeBic> bicCodes = new HashSet<>();

    @Valid
    @ImportableChild(type = ChildType.ISIN)
    @ManyToMany(cascade = CascadeType.ALL)
    @AuditedCollection(createAction = AuditedAction.RECORD_ISIN_ADDITION,
            updateAction = AuditedAction.RECORD_ISIN_MODIFICATION,
            deleteAction = AuditedAction.RECORD_ISIN_DELETION)
    @JoinTable(name = "RECORD_ISIN", joinColumns = @JoinColumn(name = "RECORD_ID"), inverseJoinColumns = @JoinColumn(name = "ISIN_ID"))
    private Set<CodeIsin> isinCodes = new HashSet<>();

    @Valid
    @ImportableChild(type = ChildType.CUSIP)
    @ManyToMany(cascade = CascadeType.ALL)
    @AuditedCollection(createAction = AuditedAction.RECORD_CUSIP_ADDITION,
            updateAction = AuditedAction.RECORD_CUSIP_MODIFICATION,
            deleteAction = AuditedAction.RECORD_CUSIP_DELETION)
    @JoinTable(name = "RECORD_CUSIP", joinColumns = @JoinColumn(name = "RECORD_ID"), inverseJoinColumns = @JoinColumn(name = "CUSIP_ID"))
    private Set<CodeCusip> cusipCodes = new HashSet<>();

    public EntityRecord() { super(RecordType.ENTITY); }

    public EntityRecord(EntityRecord other) {
        super(other);

        for (Address otherAddress: other.addresses) {
            addAddress(new Address(otherAddress));
        }

        for (CodeIban otherCode: other.ibanCodes) {
            addIbanCode(new CodeIban(otherCode));
        }

        for (CodeBic otherCode: other.bicCodes) {
            addBicCode(new CodeBic(otherCode));
        }

        for (CodeIsin otherCode: other.isinCodes) {
            addIsinCode(new CodeIsin(otherCode));
        }

        for (CodeCusip otherCode: other.cusipCodes) {
            addCusipCode(new CodeCusip(otherCode));
        }

        for (IdentityDocument otherIdentityDocument: other.identityDocuments) {
            addIdentityDocument(new IdentityDocument(otherIdentityDocument));
        }
    }

    //bics
    public Set<CodeBic> getBicCodes() { return Collections.unmodifiableSet(this.bicCodes); }

    public void addBicCode(final CodeBic code) {
        this.bicCodes.add(code);
        code.addEntityRecord(this);
    }

    public void removeBicCode(final CodeBic code) {
        this.bicCodes.remove(code);
        code.removeEntityRecord(this);
    }

    public void setBicCodes(Set<CodeBic> newCodes) {
        HashSet<CodeBic> oldCode = new HashSet<>(bicCodes);
        oldCode.forEach(this::removeBicCode);
        bicCodes.clear();
        newCodes.forEach(this::addBicCode);
    }

    //isin
    public Set<CodeIsin> getIsinCodes() { return Collections.unmodifiableSet(this.isinCodes); }

    public void addIsinCode(CodeIsin code) {
        this.isinCodes.add(code);
        code.addEntityRecord(this);
    }

    public void removeIsinCode(CodeIsin code) {
        this.isinCodes.remove(code);
        code.removeEntityRecord(this);
    }

    public void setIsinCodes(Set<CodeIsin> newCodes) {
        HashSet<CodeIsin> oldCode = new HashSet<>(isinCodes);
        oldCode.forEach(this::removeIsinCode);
        isinCodes.clear();
        newCodes.forEach(this::addIsinCode);
    }

    //cusip
    public Set<CodeCusip> getCusipCodes() { return Collections.unmodifiableSet(this.cusipCodes); }

    public void addCusipCode(final CodeCusip code) {
        this.cusipCodes.add(code);
        code.addEntityRecord(this);
    }

    public void removeCusip(final CodeCusip code) {
        this.cusipCodes.remove(code);
        code.removeEntityRecord(this);
    }

    public void setCusipCodes(Set<CodeCusip> newCodes) {
        HashSet<CodeCusip> oldCode = new HashSet<>(cusipCodes);
        oldCode.forEach(this::removeCusip);
        newCodes.forEach(this::addCusipCode);
    }
    //
}
