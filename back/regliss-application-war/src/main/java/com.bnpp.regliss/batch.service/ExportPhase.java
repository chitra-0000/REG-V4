package com.bnpp.regliss.batch.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ExportPhase {
    EXPORT_EXTRACT_DATA(1) {
        @Override
        public float calculateWeight(ExportJobParameters configuration) { return 20f; }
    },

    EXPORT_WRITE_RECORDS(2) {
        @Override
        public float calculateWeight(ExportJobParameters configuration) {
            if (configuration.getNumberOfDJFiles() == 0) {
                return 100 - getPreviousPhasesWeight(configuration);
            }
            return 100 - getPreviousPhasesWeight(configuration) - getNextPhasesWeight(configuration);
        }
    },

    EXPORT_WRITE_ASSOCIATIONS(3) {
        @Override
        public float calculateWeight(ExportJobParameters configuration) {
            if (configuration.getNumberOfDJWatchlistAmeXmlFiltersFiles() == 0) {
                return 0;
            } else {
                int maximumPhaseWeight = 20;
                float percentageOfDJFiles = (float) configuration.getNumberOfDJWatchlistAmeXmlFiltersFiles() /
                        configuration.getTotalNumberOfFiles();
                return maximumPhaseWeight * percentageOfDJFiles;
            }
        }
    },

    EXPORT_XSD_VALIDATION(4) {
        @Override
        public float calculateWeight(ExportJobParameters configuration) {
            if (configuration.getNumberOfDJXmlFiles() == 0) {
                return 0;
            } else {
                int maximumPhaseWeight = 10;
                float percentageOfDJFiles = (float) configuration.getNumberOfDJXmlFiles() / configuration.getTotalNumberOfFiles();
                return maximumPhaseWeight * percentageOfDJFiles;
            }
        }
    };

    private final int phaseIndex;

    ExportPhase(int phaseIndex) { this.phaseIndex = phaseIndex; }

    public List<ExportPhase> getPreviousPhases() {
        return Stream.of(values()).filter(v -> v.phaseIndex < this.phaseIndex)
                .collect(Collectors.toList());
    }

    public List<ExportPhase> getNextPhases() {
        return Stream.of(values()).filter(v -> v.phaseIndex > this.phaseIndex)
                .collect(Collectors.toList());
    }

    public abstract float calculateWeight(ExportJobParameters configuration);

    float getPreviousPhasesWeight(ExportJobParameters configuration) {
        float previousPhasesWeight = 0;
        List<ExportPhase> previousPhases = getPreviousPhases();
        for (ExportPhase phase : previousPhases) {
            previousPhasesWeight += phase.calculateWeight(configuration);
        }
        return previousPhasesWeight;
    }

    float getNextPhasesWeight(ExportJobParameters configuration) {
        float nextPhasesWeight = 0;
        List<ExportPhase> nextPhases = getNextPhases();
        for (ExportPhase phase : nextPhases) {
            nextPhasesWeight += phase.calculateWeight(configuration);
        }
        return nextPhasesWeight;
    }

}
