package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.exporter.writer.format.FileExportData;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Getter
public abstract class DJFileExportData extends FileExportData {

    protected DJFileExportData(Version version) { super(version); }

    public abstract Map<ExportXmlTagType, List<String>> getXmlAdditionalExportTagsValuesForRecord(RecordDjExportData recordDjExportData);

    public abstract DJFileExportData getExportDataForRecordTypes(List<RecordType> correspondingRecordTypes);
}
