package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.entity.ComputedRecordNumbers;
import com.bnpp.regliss.exporter.writer.format.addmodsup.AddModSupExportData;
import com.bnpp.regliss.monitoring.PerformanceMonitor;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.AddModSupRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.vo.AddModSupAction;
import com.bnpp.regliss.vo.AddModSupCountLine;
import com.bnpp.regliss.vo.AddModSupLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.bnpp.regliss.monitoring.PerformanceMonitor.Action.*;

@Slf4j
@Service
@ReglissBatchProfile
public class ExportAddModSupDataExtractor {
    @Autowired
    private AddModSupRepository repo;

    @Autowired
    private VersionRepository versionRepo;

    /*This method needs to be called inside a transaction because
        AddModSupExportData has Spring Data JPA streams that require a transaction to get
        next element from stream.
    */
    public AddModSupExportData extractAddModSupExportData(Long versionId) {
        List<AddModSupCountLine> elementsCounts = PerformanceMonitor.executeWithMeasurements(
                () -> repo.getAddModSupElementsCounts(versionId), EXPORT_ADD_MOD_SUP_ELEMENTS_COUNTS
        );

        List<AddModSupCountLine> recordsCounts = PerformanceMonitor.executeWithMeasurements(
                () -> repo.getAddModSupRecordsCounts(versionId), EXPORT_ADD_MOD_SUP_RECORDS_COUNTS
        );

        Map<AddModSupAction, Integer> detailsElementsCounts = PerformanceMonitor.executeWithMeasurements(
                () -> repo.getAddModSupDetailsElementsCounts(versionId), EXPORT_ADD_MOD_SUP_DETAIL_ELEMENTS_COUNTS
        );

        ComputedRecordNumbers detailsRecordsCounts = versionRepo.getExactlyOne(versionId)
                .getComputedRecordNumbers();

        Stream<AddModSupLine> summaryAddLines = PerformanceMonitor.executeWithMeasurements(
                () -> repo.getAddModSupElementsByAction(versionId, AddModSupAction.ADD), EXPORT_ADD_MOD_SUP_ELEMENTS_BY_ACTION
        );

        Stream<AddModSupLine> summaryModLines = PerformanceMonitor.executeWithMeasurements(
                () -> repo.getAddModSupElementsByAction(versionId, AddModSupAction.MOD), EXPORT_ADD_MOD_SUP_ELEMENTS_BY_ACTION
        );

        Stream<AddModSupLine> summarySupLines = PerformanceMonitor.executeWithMeasurements(
                () -> repo.getAddModSupElementsByAction(versionId, AddModSupAction.SUP), EXPORT_ADD_MOD_SUP_ELEMENTS_BY_ACTION
        );

        Stream<AddModSupLine> detailsLines = PerformanceMonitor.executeWithMeasurements(
                () -> repo.getAddModSupElements(versionId), EXPORT_ADD_MOD_SUP_ELEMENTS
        );

        return new AddModSupExportData(
                elementsCounts, recordsCounts, detailsElementsCounts, detailsRecordsCounts,
                summaryAddLines, summaryModLines, summarySupLines, detailsLines
        );
    }
}
