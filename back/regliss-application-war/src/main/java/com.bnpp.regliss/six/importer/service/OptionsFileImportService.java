package com.bnpp.regliss.six.importer.service;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.six.entity.OptionsFile;
import com.bnpp.regliss.six.dto.OptionsFileDto;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.six.importer.mapper.OptionsFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

@Service
@ReglissBatchProfile
@Slf4j
public class OptionsFileImportService {

    static final String RECORD_ELEMENT = "OptionsFile";

    @Autowired
    private OptionsFileMapper optionsFileMapper;

    @Autowired
    private SixBatchImportRunner runner;

    @Autowired
    private SixJdbcBulkWriter writer;

    public void streamAndPersist(File transformedXml, Version version, ReglissList list, long batchExecutionId)
            throws IOException, XMLStreamException {
        runner.run(new SixBatchImportRunner.RecordType<>("options", RECORD_ELEMENT, OptionsFileDto.class,
                        optionsFileMapper::mapperStructuredFile,
                        (batch, v, l) -> writer.insertBatch(batch, l.getId(), v.getId(), OptionsFile::getSixTargets, "SIX_OPT_ID")),
                transformedXml, version, list, batchExecutionId);
    }
}
