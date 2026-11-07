package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.exporter.writer.ExportPageData;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode
@AllArgsConstructor
public class DJXmlRecordsExportPageData extends ExportPageData {

    @Getter
    private final List<RecordDjExportData> recordDjExportDataPartition;

    @Getter
    private final Map<Long, Map<ExportXmlTagType, List<String>>> additionalExportTagsValuesForPartition;

}
