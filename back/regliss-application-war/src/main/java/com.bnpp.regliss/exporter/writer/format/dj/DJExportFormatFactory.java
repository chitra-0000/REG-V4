package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.entity.GeneratedFileFormat;
import com.bnpp.regliss.exporter.writer.GeneratedFileWriter;
import com.bnpp.regliss.exporter.writer.format.ExternalUrlFormatter;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ReglissBatchProfile
@Slf4j
public class DJExportFormatFactory {

    @Value("${export.dj.xml.header}")
    private String xmlHeader;

    @Autowired
    private ExternalUrlFormatter urlFormatter;

    public List<DJXmlExportFormatWriter> createAllXmlFormats(List<GeneratedFileWriter> xmlFileWriters) {
        List<DJXmlExportFormatWriter> list = new ArrayList<>();

        for (GeneratedFileWriter fileWriter : xmlFileWriters) {
            GeneratedFileFormat fileFormat = fileWriter.getFileFormat();
            log.debug("Creating file generator for format: {}", fileFormat);
            switch (fileFormat.getFileFormat()) {
                case DJ_CORE:
                    list.add(new CoreXmlExportFormatWriter(fileWriter, xmlHeader, urlFormatter));
                    break;
                case DJ_WATCHLIST:
                    list.add(new WatchlistXmlExportFormatWriter(fileWriter, xmlHeader, urlFormatter));
                    break;
                case DJ_AME:
                    list.add(new WatchlistXmlExportFormatWriter(fileWriter, xmlHeader, urlFormatter));
                    break;
                default:
                    throw new NotImplementedException("Not a data file: " + fileFormat.getFileFormat().name());
            }
        }
        return list;
    }

    public List<WatchlistAmeCsvExportFormatWriter> createAllCsvFormats(List<GeneratedFileWriter> csvFileWriters) {
        List<WatchlistAmeCsvExportFormatWriter> list = new ArrayList<>();

        for (GeneratedFileWriter fileWriter : csvFileWriters) {
            GeneratedFileFormat fileFormat = fileWriter.getFileFormat();
            log.debug("Creating file generator for format: {}", fileFormat);
            switch (fileFormat.getFileFormat()) {
                case DJ_WATCHLIST:
                    list.add(new WatchlistAmeCsvExportFormatWriter(fileWriter));
                    break;
                case DJ_AME:
                    list.add(new WatchlistAmeCsvExportFormatWriter(fileWriter));
                    break;
                default:
                    throw new NotImplementedException("CSV export not allowed for format: " + fileFormat.getFileFormat().name());
            }
        }
        return list;
    }

}
