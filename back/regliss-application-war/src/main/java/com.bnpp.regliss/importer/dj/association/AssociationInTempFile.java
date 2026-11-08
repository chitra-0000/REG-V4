package com.bnpp.regliss.importer.dj.association;

import lombok.Data;

@Data
class AssociationInTempFile {
    private final String externalReference;
    private final long position;
    private final int length;
}
