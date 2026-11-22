package com.bnpp.regliss.repository;

import com.bnpp.regliss.vo.AssociationExternalReference;

import java.util.HashMap;
import java.util.Map;

/** For memory saving*/
public class AssociationExternalReferenceCanonicFactory {
    private final Map<String, AssociationExternalReference> canonicMap = new HashMap<>();

    public AssociationExternalReference getCanonic(String value) {
        return canonicMap.computeIfAbsent(value, AssociationExternalReference::new);
    }
}

