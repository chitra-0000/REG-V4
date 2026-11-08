package com.bnpp.regliss.importer.dj.association;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class CoreAssociationTempStorage implements IAssociationTempStorage {
    @Override
    public void extractToTempFile(Supplier<? extends InputStream> xmlInputStreamSupplier) {
        // Nothing to do, we don't yet extract Associations from CORE files
    }
    @Override
    public IAssociationReader openReader() { return new NoDataReader(); }

    public class NoDataReader implements IAssociationReader {

        @Override
        public void close() {
            // Nothing to close
        }

        @Override
        public String readSnippet(String externalReference) { return null; }

    }
}

