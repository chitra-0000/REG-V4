package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.entity.referential.record.RecordType;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/** As per the XSDs for DJ XML files (WL, Ame and Core), the values of this enum are the expected tag names at the beginning of the XML snippets
 *    djExportOrder - defines the order of the tag described by the XSDs for imported/exported DJ files
 *    correspondingRecordTypes - defines the record types that could be imported/exported from the tag name (based on the mappings defined in dow-
 */
public enum DJRecordTagName {

    PERSON(1, singletonList(RecordType.PERSON)),
    ENTITY(2, asList(RecordType.ENTITY, RecordType.VESSEL));

    private final int djExportOrder;
    private final List<RecordType> correspondingRecordTypes;

    DJRecordTagName(int djExportOrder, List<RecordType> correspondingRecordTypes) {
        this.djExportOrder = djExportOrder;
        this.correspondingRecordTypes = correspondingRecordTypes;
    }

    public int getDjImportExportOrder() { return djExportOrder; }

    public List<RecordType> getCorrespondingRecordTypes() { return correspondingRecordTypes; }

    public static List<DJRecordTagName> getOrderedRecordTagNamesForExport() {
        return Arrays.stream(DJRecordTagName.values())
                .sorted(comparing(DJRecordTagName::getDjImportExportOrder))
                .collect(toList());
    }
}
