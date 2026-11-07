package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.vo.export.ExportElementData;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class DJXmlRecordExportElementData implements ExportElementData {
    private RecordDjExportData recordDjExportData;
    private Map<ExportXmlTagType, List<String>> additionalExportTagsValues;
}
