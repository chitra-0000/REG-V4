package com.bnpp.regliss.exporter.writer.format.addmodsup;

import com.bnpp.regliss.entity.ComputedRecordNumbers;
import com.bnpp.regliss.vo.AddModSupAction;
import com.bnpp.regliss.vo.AddModSupCountLine;
import com.bnpp.regliss.vo.AddModSupLine;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Data
public class AddModSupExportData {

    private final List<AddModSupCountLine> elementsCounts;
    private final List<AddModSupCountLine> recordsCounts;
    private final Map<AddModSupAction, Integer> detailsElementsCounts;
    private final ComputedRecordNumbers detailsRecordsCounts;
    private final Stream<AddModSupLine> summaryAddLines;
    private final Stream<AddModSupLine> summaryModLines;
    private final Stream<AddModSupLine> summarySupLines;
    private final Stream<AddModSupLine> detailsLines;

    public AddModSupExportData(List<AddModSupCountLine> elementsCounts, List<AddModSupCountLine> recordsCounts,
                               Map<AddModSupAction, Integer> detailsElementsCounts, ComputedRecordNumbers detailsRecordsCounts,
                               Stream<AddModSupLine> summaryAddLines, Stream<AddModSupLine> summaryModLines,
                               Stream<AddModSupLine> summarySupLines, Stream<AddModSupLine> detailsLines) {
        this.elementsCounts = elementsCounts;
        this.recordsCounts = recordsCounts;
        this.detailsElementsCounts = detailsElementsCounts;
        this.detailsRecordsCounts = detailsRecordsCounts;
        this.summaryAddLines = summaryAddLines;
        this.summaryModLines = summaryModLines;
        this.summarySupLines = summarySupLines;
        this.detailsLines = detailsLines;
    }

    public int getTotalNumberOfElements() {
        return detailsElementsCounts.values().stream()
                .mapToInt(Integer::intValue).sum();
    }

    public Integer getDetailsElementsCountForAction(AddModSupAction action) { return detailsElementsCounts.get(action); }

    public Stream<AddModSupLine> getSummaryLinesForAction(AddModSupAction action) {
        switch (action) {
        case ADD:
            return summaryAddLines;
        case MOD:
            return summaryModLines;
        case SUP:
            return summarySupLines;
        default:
            throw new IllegalArgumentException("Not a valid action: " + action);
        }
    }

    public Long getDetailsRecordsCountsForAction(AddModSupAction action) {
        switch (action) {
        case ADD:
            return detailsRecordsCounts.getAdd();
        case MOD:
            return detailsRecordsCounts.getModForSynthesis();
        case SUP:
            return detailsRecordsCounts.getSup();
        default:
            throw new IllegalArgumentException("Not a valid action: " + action);
        }
    }
}
