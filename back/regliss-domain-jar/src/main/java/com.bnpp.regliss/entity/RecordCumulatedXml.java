package com.bnpp.regliss.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class RecordCumulatedXml {

    @Getter
    private final String recordXml;
    private final String associationXml;


    public String getExternalReference(ImportFileType format) {
        return XmlSnippetExternalReferenceExtractor.extractIdFromXml(recordXml, format);
    }


    public Optional<String> getAssociationXml() { return Optional.ofNullable(associationXml); }
}
