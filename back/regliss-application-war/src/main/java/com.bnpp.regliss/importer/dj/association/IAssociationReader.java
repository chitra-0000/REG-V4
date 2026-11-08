package com.bnpp.regliss.importer.dj.association;

public interface IAssociationReader extends AutoCloseable {

    String readSnippet(String externalReference);
    void close();

}
