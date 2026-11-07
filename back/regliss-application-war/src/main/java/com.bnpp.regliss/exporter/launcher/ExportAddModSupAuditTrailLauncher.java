package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.entity.AddModSupAuditTrailFile;
import com.bnpp.regliss.exporter.writer.FileGenerator;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.AddModSupRepository;
import com.bnpp.regliss.vo.AddModSupAction;
import com.bnpp.regliss.vo.AddModSupLineForSynthesis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@ReglissBatchProfile
public class ExportAddModSupAuditTrailLauncher {

    @Autowired
    private AddModSupRepository addModSupRepo;

    @Autowired
    private FileGenerator generator;

    @Autowired
    private BatchProgressService progressService;

    public File generateAddModSupAuditTrailFile(AddModSupAuditTrailFile fileToGenerate, long batchExecutionId) {
        ExportJobParameters exportParameters = new ExportJobParameters(fileToGenerate, batchExecutionId);
        Long versionId = fileToGenerate.getVersion().getId();

        Map<AddModSupAction, List<AddModSupLineForSynthesis>>  dataToExport = dataToExport(versionId);
        progressService.incrementBatchExportProgressAfterExtractData(exportParameters, ExportQueryType.ADD_MOD_SUP_AUDIT_TRAIL_QUERY);

        return generator.tryGenerateAddModSupAuditTrailFile(dataToExport);
    }

    private Map<AddModSupAction, List<AddModSupLineForSynthesis>> dataToExport(Long versionId){
        Map<AddModSupAction, List<AddModSupLineForSynthesis>>  dataToExport = new LinkedHashMap<>();
        dataToExport.put(AddModSupAction.ADD, addModSupRepo.getAllAddModSupElementsByAction(versionId, AddModSupAction.ADD));
        dataToExport.put(AddModSupAction.MOD, addModSupRepo.getAllAddModSupElementsByAction(versionId, AddModSupAction.MOD));
        dataToExport.put(AddModSupAction.SUP, addModSupRepo.getAllAddModSupElementsByAction(versionId, AddModSupAction.SUP));
        return dataToExport;
    }
}
