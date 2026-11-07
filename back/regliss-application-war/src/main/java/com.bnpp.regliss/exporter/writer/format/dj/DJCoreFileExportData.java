package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.referential.FileExportMode;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.exporter.writer.format.ExportFormatWriter;
import com.bnpp.regliss.vo.RecordIdType;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@EqualsAndHashCode(callSuper = true)
@Getter
public class DJCoreFileExportData extends DJFileExportData {
    private final Set<RecordIdType> recordIds;
    private final FileExportMode exportMode;

    public DJCoreFileExportData(Set<RecordIdType> recordIds, FileExportMode exportMode, Version version) {
        super(version);
        this.recordIds = recordIds;
        this.exportMode = exportMode;
    }

    @Override
    public Set<Long> getRecordsIds() { return recordIds.stream().map(RecordIdType::getId).collect(Collectors.toSet()); }


    @Override
    public Map<ExportXmlTagType, List<String>> getXmlAdditionalExportTagsValuesForRecord(RecordDjExportData recordDjExportData) {
        Map<ExportXmlTagType, List<String>> result = new EnumMap<>(ExportXmlTagType.class);
        result.put(ExportXmlTagType.SOURCE, singletonList(recordDjExportData.getUrlReference()));
        return result;
    }

    @Override
    public String getExportType() { return "CORE export, MODE: " + exportMode; }

    @Override
    public List<? extends ExportFormatWriter> getWritersForRecord(Long recordId, List<? extends ExportFormatWriter> pagedFormatWriters) {
        return pagedFormatWriters;
    }

    @Override
    public DJFileExportData getExportDataForRecordTypes(List<RecordType> correspondingRecordTypes) {
        Set<RecordIdType> recordIdsTypes = recordIds.stream()
                .filter(rit -> correspondingRecordTypes.contains(rit.getType()))
                .collect(Collectors.toSet());

        return new DJCoreFileExportData(recordIdsTypes, exportMode, getVersion());
    }
}
