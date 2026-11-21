package com.bnpp.regliss.six.importer.service;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.six.entity.StructuredFile;
import com.bnpp.regliss.six.dto.StructuredFileDto;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.six.importer.mapper.StructuredFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

@Service
@ReglissBatchProfile
@Slf4j
public class StructuredFileImportService {

    static final String RECORD_ELEMENT = "STRUCTURED_FILE";

    @Autowired
    private StructuredFileMapper structuredFileMapper;

    @Autowired
    private SixBatchImportRunner runner;

    @Autowired
    private SixJdbcBulkWriter writer;

    public void streamAndPersist(File transformedXml, Version version, ReglissList list, long batchExecutionId)
            throws IOException, XMLStreamException {
        runner.run(new SixBatchImportRunner.RecordType<>("structure", RECORD_ELEMENT, StructuredFileDto.class,
                        structuredFileMapper::mapperStructuredFile,
                        (batch, v, l) -> writer.insertBatch(batch, l.getId(), v.getId(), StructuredFile::getSixTargets, "SIX_STRUCT_ID")),
                transformedXml, version, list, batchExecutionId);
    }
}
