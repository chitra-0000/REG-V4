package com.bnpp.regliss.batch.service;

import com.bnpp.regliss.exporter.writer.format.dj.DJRecordTagName;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.bnpp.regliss.batch.service.ExportPhase.EXPORT_EXTRACT_DATA;
import static com.bnpp.regliss.batch.service.ExportPhase.EXPORT_XSD_VALIDATION;

@Slf4j
public class ExportProgressCalculator {

    private ExportProgressCalculator(){

    }

    public static float getPercentToIncrementAfterFetchRecords(ExportJobParameters jobParameters) {
        int numberOfQueries = jobParameters.getNumberOfQueries();
        float percentToIncrement = formatValue(EXPORT_EXTRACT_DATA.calculateWeight(jobParameters) / numberOfQueries);
        logProgress(percentToIncrement, EXPORT_EXTRACT_DATA, jobParameters);
        return percentToIncrement;
    }

    public static float getPercentToIncrementForXsdValidation(ExportJobParameters jobParameters) {
        float percentToIncrement = formatValue(EXPORT_XSD_VALIDATION.calculateWeight(jobParameters) / jobParameters.getNumberOfDJXmlFiles());
        logProgress(percentToIncrement, EXPORT_XSD_VALIDATION, jobParameters);
        return percentToIncrement;
    }

    public static float getPercentToIncrementForWritingPage(ExportPageType pageType, int numberOfWrittenFiles, int totalNumberOfExportedElements,
                                                            int pageSize, ExportJobParameters jobParameters) {
        float pageTypeWeightForAFile = getPhaseWeightForAFile(pageType.getExportPhase(), jobParameters) /
                pageType.getAllPageTypesForSamePhaseAndExtension().size();
        float pageTypeWeightForAPage = pageTypeWeightForAFile / getNumberOfPages(totalNumberOfExportedElements, pageSize);
        float percentToIncrement = formatValue(pageTypeWeightForAPage * numberOfWrittenFiles);
        logProgress(percentToIncrement, pageType.getExportPhase(), jobParameters);
        return percentToIncrement;
    }

    public static float getPercentToIncrementForWritingAllPagesAtOnceOrNoPages(ExportPhase exportPhase, int numberOfWrittenFiles,
                                                                              ExportJobParameters jobParameters) {
        float percentToIncrement = formatValue(getPhaseWeightForAFile(exportPhase, jobParameters) * numberOfWrittenFiles);
        logProgress(percentToIncrement, exportPhase, jobParameters);
        return percentToIncrement;
    }

    private static float getPhaseWeightForAFile(ExportPhase exportPhase, ExportJobParameters jobParameters) {
        float exportPhaseWeight = exportPhase.calculateWeight(jobParameters);
        if(exportPhaseWeight != 0.0f) {
            return exportPhaseWeight / getNumberOfFilesForPhase(exportPhase, jobParameters);
        }
        return exportPhaseWeight;
    }

    private static float getNumberOfFilesForPhase(ExportPhase phaseType, ExportJobParameters jobParameters) {
        switch (phaseType) {
            case EXPORT_WRITE_RECORDS:
                return jobParameters.getNumberOfRegularFiles() + jobParameters.getNumberOfAddModSupFiles() +
                        jobParameters.getNumberOfDJFilesNotZip() * DJRecordTagName.values().length + (float)jobParameters.getNumberOfDJFileZip();
            case EXPORT_WRITE_ASSOCIATIONS:
                return jobParameters.getNumberOfDJWatchlistAmeXmlFiltersFiles() * (float)DJRecordTagName.values().length;
            default:
                throw new IllegalArgumentException("Page weight not computed for page type: " + phaseType);
        }
    }

    private static int getNumberOfPages(int numberOfExportedElements, int pageSize) {
        if (numberOfExportedElements % pageSize == 0) {
            return numberOfExportedElements / pageSize;
        } else {
            return numberOfExportedElements / pageSize + 1;
        }
    }

    private static void logProgress(float percentToIncrement, ExportPhase phase, ExportJobParameters jobParameters) {
        float overallPercentageAfterStep = jobParameters.incrementOverallPercentage(percentToIncrement);
        if (overallPercentageAfterStep != jobParameters.getLastPrintedOverallPercentage()) {
            log.info("Batch job execution id {} currently at: {} %. Last executed step: {}", jobParameters.getBatchJobExecId(),
                    overallPercentageAfterStep, phase);
            jobParameters.updateLastPrintedOverallPercentage(overallPercentageAfterStep);
        }
    }

    private static float formatValue(float val) {
        return BigDecimal.valueOf(val)
                .setScale(4, RoundingMode.DOWN).floatValue();
    }
}
