package com.bnpp.regliss.utils;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.validation.Validable;
import com.bnpp.regliss.vo.TruncateStringFieldsVO;
import lombok.Getter;

import javax.validation.ConstraintViolation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.bnpp.regliss.ReglissException.ErrorCode.ERROR_CONSTRAINT_THRESHOLD_VIOLATION;
import static com.bnpp.regliss.ReglissException.ErrorCode.ERROR_CONSTRAINT_VIOLATION;
import static java.util.stream.Collectors.*;

public class BusinessDataControls {
    @Getter
    private final int constraintViolationsErrorsThreshold;
    @Getter
    private final int constraintViolationsWarningsThreshold;
    @Getter
    private final int truncatedFieldsThreshold;
    @Getter
    private Set<ConstraintViolation<Validable>> constraintViolationsErrors = new HashSet<>();
    @Getter
    private Set<ConstraintViolation<Validable>> constraintViolationsWarnings = new HashSet<>();
    @Getter
    private Set<TruncatedRecordFieldsValues> truncatedEntities = new HashSet<>();
    private boolean isManualImport;

    public BusinessDataControls(int constraintViolationsErrorsThreshold, int constraintViolationsWarningsThreshold, int truncatedFieldsThreshold, boolean isManualImport){
        this.constraintViolationsErrorsThreshold = constraintViolationsErrorsThreshold;
        this.constraintViolationsWarningsThreshold = constraintViolationsWarningsThreshold;
        this.truncatedFieldsThreshold = truncatedFieldsThreshold;
        this.isManualImport = isManualImport;
    }

    public synchronized void addRecordValidationsInSingleThread(Record record){
        addErrorConstraintViolationsForRecordUntilThreshold(record);
        addWarningConstraintViolationsForRecordUntilThreshold(record);
        addTruncatedFieldsForRecordUntilThreshold(record);
    }

    public void stopIfErrorThresholdExceeded() {
        if (wasConstraintViolationsErrorsThresholdExceeded()) {
            throw new ReglissException(ERROR_CONSTRAINT_THRESHOLD_VIOLATION + " for records with external references : " + extractExternalReferences());
        }
    }

    public void stopIfAnyConstraintViolationsErrors() {
        if (hasConstraintViolationsErrors()) {
            throw new ReglissException(ERROR_CONSTRAINT_VIOLATION + " for records with external references : " + extractExternalReferences());
        }
    }

    public boolean wasConstraintViolationsWarningsThresholdExceeded() {
        return constraintViolationsWarnings.size() == constraintViolationsWarningsThreshold;
    }

    public boolean wasConstraintViolationsErrorsThresholdExceeded() {
        return constraintViolationsErrors.size() == constraintViolationsErrorsThreshold;
    }

    public boolean wasTruncatedFieldsThresholdExceeded() {
        int numberOfTruncatedFields = truncatedEntities.stream().mapToInt(e -> e.getTruncateStringList().size()).sum();
        return numberOfTruncatedFields == truncatedFieldsThreshold;
    }

    public boolean hasConstraintViolationsErrorsOrWarnings(){
        return hasConstraintViolationsErrors() || !constraintViolationsWarnings.isEmpty();
    }

    public boolean hasNoConstraintViolationsErrors() { return !hasConstraintViolationsErrors(); }

    public boolean hasConstraintViolationsErrors() { return !constraintViolationsErrors.isEmpty(); }

    private void addTruncatedFieldsForRecordUntilThreshold(Record record) {
        int numberOfTruncatedFields = truncatedEntities.stream().mapToInt(e -> e.getTruncateStringList().size()).sum();
        int numberOfNewTruncatedFieldsAllowed = truncatedFieldsThreshold - numberOfTruncatedFields;

        if(numberOfTruncatedFields < truncatedFieldsThreshold) {
            Set<TruncatedRecordFieldsValues> truncateStringFieldsEntityAndChildren = EntityUtils.getTruncateStringFieldsEntityAndChildren(record);
            for(TruncatedRecordFieldsValues truncatedEntity : truncateStringFieldsEntityAndChildren) {
                if(numberOfNewTruncatedFieldsAllowed > 0) {
                    List<TruncateStringFieldsVO> limitedTruncatedFieldsForCurrentEntity = limitTruncatedFieldsListToAllowedNumberOfElements(truncatedEntity.getTruncateStringList(),
                            numberOfNewTruncatedFieldsAllowed);
                    truncatedEntity.setTruncateStringList(limitedTruncatedFieldsForCurrentEntity);
                    truncatedEntities.add(truncatedEntity);
                    numberOfNewTruncatedFieldsAllowed -= limitedTruncatedFieldsForCurrentEntity.size(); //when limitation occurs, this variable will become 0
                }
            }
        }
    }

    private void addErrorConstraintViolationsForRecordUntilThreshold(Record record) {
        if(constraintViolationsErrors.size() < constraintViolationsErrorsThreshold) {
            int numberOfNewErrorsAllowed = constraintViolationsErrorsThreshold - constraintViolationsErrors.size();
            Set<ConstraintViolation<Validable>> constraintViolationSet;
            if(isManualImport){
                constraintViolationSet = record.getSemiAutoValidationErrors();
            } else {
                constraintViolationSet = record.getValidationErrors();
            }
            Set<ConstraintViolation<Validable>> newErrors = limitConstraintViolationListToAllowedNumberOfElements(constraintViolationSet, numberOfNewErrorsAllowed);
            constraintViolationsErrors.addAll(newErrors);
        }
    }

    private void addWarningConstraintViolationsForRecordUntilThreshold(Record record) {
        if(constraintViolationsWarnings.size() < constraintViolationsWarningsThreshold) {
            int numberOfNewWarningsAllowed = constraintViolationsWarningsThreshold - constraintViolationsWarnings.size();
            Set<ConstraintViolation<Validable>> newWarnings = limitConstraintViolationListToAllowedNumberOfElements(record.getValidationWarnings(), numberOfNewWarningsAllowed);
            constraintViolationsWarnings.addAll(newWarnings);
        }
    }

    private Set<ConstraintViolation<Validable>> limitConstraintViolationListToAllowedNumberOfElements(Set<ConstraintViolation<Validable>> constraintViolationsList, int numberOfConstraintViolationsAllowed) {

        if(constraintViolationsList.size() > numberOfConstraintViolationsAllowed) {
            return constraintViolationsList.stream()
                    .limit(numberOfConstraintViolationsAllowed)
                    .collect(toSet());
        }

        return constraintViolationsList;
    }

    private List<TruncateStringFieldsVO> limitTruncatedFieldsListToAllowedNumberOfElements(List<TruncateStringFieldsVO> truncatedFieldsList, int numberOfTruncatedFieldsAllowed) {
        if(truncatedFieldsList.size() > numberOfTruncatedFieldsAllowed) {
            return truncatedFieldsList.stream()
                    .limit(numberOfTruncatedFieldsAllowed)
                    .collect(toList());
        }

        return truncatedFieldsList;
    }

    private String extractExternalReferences(){
        return constraintViolationsErrors.stream().map(elem -> elem.getRootBean().getBusinessId())
                .distinct()
                .collect(joining("<br/>"));
    }
}
