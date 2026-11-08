package com.bnpp.regliss.importer.dj.association;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public interface IAssociationTempStorage {

    void extractToTempFile(Supplier<? extends InputStream> xmlInputStreamSupplier);

    IAssociationReader openReader();

}
