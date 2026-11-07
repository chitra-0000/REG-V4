package com.bnpp.regliss.exporter.writer.format.addmodsup;

import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.monitoring.PerformanceMonitor;
import com.bnpp.regliss.monitoring.PerformanceMonitor.Action;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@ReglissBatchProfile
public class AddModSupExportWriter {

    @Autowired
    private AddModSupExcelExporter addModSupExcelExporter;

    @Value("${export.add.mod.sup.excel.template}")
    private File excelTemplate;

    public void generateAddModSupFile(File tempFile, AddModSupExportData addModSupExportData, ExportJobParameters exportJobParameters) {
        PerformanceMonitor.before(Action.EXPORT_ADD_MOD_SUP_WRITE_DATA_TO_TEMPLATE);
        addModSupExcelExporter.generateExcelFile(excelTemplate, tempFile,
                addModSupExportData,
                exportJobParameters);
        PerformanceMonitor.after(Action.EXPORT_ADD_MOD_SUP_WRITE_DATA_TO_TEMPLATE);

        PerformanceMonitor.printStats();
        PerformanceMonitor.clearStats();
    }

}

