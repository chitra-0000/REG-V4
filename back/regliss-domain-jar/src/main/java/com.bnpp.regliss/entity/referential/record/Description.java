package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.entity.record.RecordAwareChild;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;

import javax.persistence.MappedSuperclass;
import java.util.HashSet;
import java.util.Set;

@MappedSuperclass
public abstract class Description extends RecordReferential implements RecordAwareChild{

    public Description() {  }

    public Description(Description other) { super(other); }

    public Description(String label) { super(label); }

    public Description(String label, ReferentialBusinessConcept businessConcept) { super(label, businessConcept); }

    public abstract Description getParent();

    public abstract String computeBusinessKey();

    public Set<Description> getMeAndAllAncestors() {
        Set<Description> set = new HashSet<>();
        set.add(this);
        if (getParent() != null) {
            set.addAll(getParent().getMeAndAllAncestors());
        }
        return set;
    }

    public boolean isPEP() { return ReferentialBusinessConcept.RECORD_CATEGORY_PEP.equals(businessConcept); }

    public boolean isOnPEPSubtree() {
        if (isPEP()) return true;
        if (getParent() == null) return false;
        return getParent().isOnPEPSubtree();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{id=" + getId() + ", instance=" + System.identityHashCode(this) +"}";
    }
}
