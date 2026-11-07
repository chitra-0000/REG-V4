package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.vo.export.ExportElementData;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;
import lombok.Value;

@Value
public class DJCsvRecordExportElementData implements ExportElementData {
    private RecordDjExportData recordDjExportData;
}
