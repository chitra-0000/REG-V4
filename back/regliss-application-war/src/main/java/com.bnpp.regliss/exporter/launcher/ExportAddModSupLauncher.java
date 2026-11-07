package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.entity.AddModSupExportFile;
import com.bnpp.regliss.exporter.writer.FileGenerator;
import com.bnpp.regliss.exporter.writer.format.addmodsup.AddModSupExportData;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@ReglissBatchProfile
public class ExportAddModSupLauncher {

    @Autowired
    private FileGenerator generator;

    @Autowired
    private ExportDataExtractor exportDataExtractor;

    @Autowired
    private BatchProgressService progressService;

    public File generateAddModSupFile(AddModSupExportFile fileToGenerate, long batchExecutionId) {
        ExportJobParameters exportParameters = new ExportJobParameters(fileToGenerate, batchExecutionId);
        Long versionId = fileToGenerate.getVersion().getId();

        AddModSupExportData addModSupExportData = exportDataExtractor.extractAddModSupExportData(versionId);
        progressService.incrementBatchExportProgressAfterExtractData(exportParameters, ExportQueryType.ADD_MOD_SUP_EXPORT_QUERY);

        return generator.tryGenerateAddModSupFile(addModSupExportData, exportParameters);
    }

}
