package com.bnpp.regliss.six.importer.service;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.six.entity.InstrumentFile;
import com.bnpp.regliss.six.dto.InstrumentFilesDto;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.six.importer.mapper.InstrumentFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

@Service
@ReglissBatchProfile
@Slf4j
public class InstrumentFileImportService {

    static final String RECORD_ELEMENT = "INSTRUMENT_FILE";

    @Autowired
    private InstrumentFileMapper instrumentFileMapper;

    @Autowired
    private SixBatchImportRunner runner;

    @Autowired
    private SixJdbcBulkWriter writer;

    public void streamAndPersist(File transformedXml, Version version, ReglissList list, long batchExecutionId)
            throws IOException, XMLStreamException {
        runner.run(new SixBatchImportRunner.RecordType<>("instrument", RECORD_ELEMENT, InstrumentFilesDto.class,
                        instrumentFileMapper::mapperStructuredFile,
                        (batch, v, l) -> writer.insertBatch(batch, l.getId(), v.getId(), InstrumentFile::getSixTargets, "SIX_INSTRU_ID")),
                transformedXml, version, list, batchExecutionId);
    }
}
