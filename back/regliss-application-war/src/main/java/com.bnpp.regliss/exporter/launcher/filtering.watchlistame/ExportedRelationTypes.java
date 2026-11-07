package com.bnpp.regliss.exporter.launcher.filtering.watchlistame;

import com.bnpp.regliss.vo.RecordAssociationVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ExportedRelationTypes {
    public static final ExportedRelationTypes ALL_RELATIONS = new ExportedRelationTypes(Collections.emptyList());
    public static final ExportedRelationTypes NO_RELATION = new ExportedRelationTypes(null);
    private final List<Long> relationTypeIds;

    public ExportedRelationTypes(List<Long> relationTypeIds) { this.relationTypeIds = relationTypeIds; }

    public ExportedRelationTypes merge(ExportedRelationTypes other) {
        if (other.isAll() || this.isAll()) {
            return ALL_RELATIONS;
        }
        List<Long> newIds = new ArrayList<>();
        if (!this.isNothing()) {
            newIds.addAll(this.relationTypeIds);
        }
        if (!other.isNothing()) {
            newIds.addAll(other.relationTypeIds);
        }
        return new ExportedRelationTypes(newIds);
    }

    public boolean isAll() { return ALL_RELATIONS.equals(this); }

    public boolean isNothing() { return NO_RELATION.equals(this); }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        ExportedRelationTypes other = (ExportedRelationTypes) obj;
        List<Long> otherRelationTypeIds = other.relationTypeIds;
        return Objects.equals(this.relationTypeIds, otherRelationTypeIds);
    }

    @Override
    public int hashCode() { return Objects.hash(this.relationTypeIds); }

    public boolean matches(RecordAssociationVO association) {
        if (isAll()) {
            return true;
        } else if (isNothing()) {
            return false;
        } else {
            return relationTypeIds.contains(association.getRelationTypeId());
        }
    }
}

