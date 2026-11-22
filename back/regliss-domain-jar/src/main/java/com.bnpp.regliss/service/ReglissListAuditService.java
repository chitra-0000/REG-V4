package com.bnpp.regliss.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.bnpp.regliss.entity.six.entity.SixFilters;
import com.bnpp.regliss.entity.six.entity.SixSubFilters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.DJExportFilter;
import com.bnpp.regliss.entity.DJExportSubfilter;
import com.bnpp.regliss.entity.Log;
import com.bnpp.regliss.entity.LogDetail;
import com.bnpp.regliss.entity.MappingFile;
import com.bnpp.regliss.entity.ObjectType;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.repository.AuditRepository;
import com.bnpp.regliss.repository.LogRepository;
import com.bnpp.regliss.search.ReglissListSearchCriteria;
import com.bnpp.regliss.utils.AuditUtil;

@Service
public class ReglissListAuditService {

    private static final String CREATE = "Create";
    private static final String DELETE = "Delete";
    private static final String FALSE = "False";

    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private LogRepository logRepo;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ConsultedEntitiesInSession session;

    public void auditCreate(ReglissList list) {
        auditRepo.save(fill(new Audit(AuditedAction.LIST_CREATION), list));
        if (list.isActive()) {
            auditRepo.save(fill(new Audit(AuditedAction.LIST_ACTIVATION), list));
        }
    }


    public void auditUpdate(ReglissList oldList, ReglissList newList) {
        auditActivation(oldList, newList);
        auditFieldsChanged(oldList, newList);
    }


    public void auditFilterUpdate(ReglissList oldList, ReglissList newList, String filterName) {
        auditFieldsChanged(oldList, newList, audit -> configureFilterModificationAudit(audit, filterName));
    }

    public void auditSubfilterUpdate(DJExportSubfilter oldSubfilter, DJExportSubfilter updatedSubfilter, ReglissList list,  String subfilterName) {
        auditSubfilterFieldsChanged(oldSubfilter, updatedSubfilter,list,  audit -> configureFilterModificationAudit(audit, subfilterName));
    }

    public void auditSubfilterFieldsChanged(DJExportSubfilter oldFilter, DJExportSubfilter filter, ReglissList list,  Consumer<Audit> auditConfigurer) {
        List<Audit> audits = ReflectionComparator.compare(oldFilter, filter);
        for (Audit audit : audits) {
            auditConfigurer.accept(audit);
            auditRepo.save(fillSubfilter(audit, oldFilter, list));
        }
    }


    public void auditFieldsChanged(ReglissList oldList, ReglissList newList, Consumer<Audit> auditConfigurer) {
        List<Audit> audits = ReflectionComparator.compare(oldList, newList);
        for (Audit audit : audits) {
            auditConfigurer.accept(audit);
            auditRepo.save(fill(audit, oldList));
        }
    }

    private void auditFieldsChanged(ReglissList oldList, ReglissList newList) {
        auditFieldsChanged(oldList, newList, this::configureListModificationAudit);
    }

    private void configureListModificationAudit(Audit audit) {
        if(audit.getAuditedAction() == null){
            audit.setAuditedAction(AuditedAction.LIST_MODIFICATION);
        }
    }

    private void configureSubfilterModificationAudit(Audit audit, String subfilterName) {
        audit.setOldValue(prefixValueSubFilterName(audit.getOldValue(), subfilterName));
        audit.setNewValue(prefixValueSubFilterName(audit.getNewValue(), subfilterName));
    }

    private void configureFilterModificationAudit(Audit audit, String filterName) {
        audit.setOldValue(prefixValueWithFilterName(audit.getOldValue(), filterName));
        audit.setNewValue(prefixValueWithFilterName(audit.getNewValue(), filterName));
    }

    private String prefixValueSubFilterName(String value, String subfilterName) {
        if(value == null) {
            return value;
        }
        return subfilterName;
    }

    private String prefixValueWithFilterName(String value, String filterName) {
        if(value == null) {
            return value;
        }
        return filterName + " / " + value.replace( /* target: */ ",",  /* replacement: */ " /");
    }

    public void auditFilterActivation(DJExportFilter filter) {
        Audit audit = new Audit(AuditedAction.LIST_FILTER_ACTIVATION);
        fill(audit, filter.getList());
        audit.setAuditedField(AuditedFieldCode.FILTER_ACTIVE);
        audit.setOldValue(prefixValueWithFilterName(FALSE, filter.getFilterName()));
        audit.setNewValue(prefixValueWithFilterName( /* value: */ "True", filter.getFilterName()));
        auditRepo.save(audit);
    }

    public void auditFilterDeactivation(DJExportFilter filter) {
        Audit audit = new Audit(AuditedAction.LIST_FILTER_DEACTIVATION);
        fill(audit, filter.getList());
        audit.setAuditedField(AuditedFieldCode.FILTER_ACTIVE);
        audit.setOldValue(prefixValueWithFilterName( /* value: */ "True", filter.getFilterName()));
        audit.setNewValue(prefixValueWithFilterName(FALSE, filter.getFilterName()));
        auditRepo.save(audit);
    }

    public void auditFilterDeletion(DJExportFilter filter) {
        Audit audit = new Audit(AuditedAction.LIST_FILTER_DELETION);
        fill(audit, filter.getList());
        audit.setAuditedField(AuditedFieldCode.FILTER_DELETE);
        audit.setOldValue(prefixValueWithFilterName(FALSE, filter.getFilterName()));
        audit.setNewValue(prefixValueWithFilterName( /* value: */ "True", filter.getFilterName()));
        auditRepo.save(audit);
    }

    public void auditAddDeleteSubfilters(ReglissList oldList, EntityCollectionDiff<DJExportSubfilter> subfilterDiff,
                            String filterName, String action) {
        List<Audit> audits = new ArrayList<>();
        if (action.matches(CREATE)) {
            for (DJExportSubfilter toCreate : subfilterDiff.getCreatedElements()) {
                audits.add(ReflectionComparator.createAddInCollectionAudit(toCreate,
                        AuditedAction.LIST_FILTER_SUBFILTER_ADDITION));
            }
        }
        else if (action.matches(DELETE)) {
            for (DJExportSubfilter toDelete : subfilterDiff.getDeletedElements()) {
                audits.add(ReflectionComparator.createDeleteFromCollectionAudit(toDelete,
                        AuditedAction.LIST_FILTER_SUBFILTER_DELETION));
            }
        }

        for (Audit audit : audits) {
            configureSubfilterModificationAudit(audit, filterName);
            auditRepo.save(fill(audit, oldList));
        }
    }

    // TODO: [GAP - lines 165-205 not captured in screenshots]

    public void auditUpdateMappingFile(MappingFile oldMappingFile, MappingFile newMappingFile) {
        Audit audit = new Audit();
        audit.setAuditedField(AuditedFieldCode.LIST_MAPPING_FILE_NAME);
        audit.setOldValue(oldMappingFile.getFileName());
        audit.setNewValue(newMappingFile.getFileName());
        audit.setAuditedAction(AuditedAction.LIST_MAPPING_FILE_MODIFICATION);
        audit.setChildId(newMappingFile.getId());
        audit.setChild(newMappingFile);
        fill(audit, oldMappingFile.getList());
        auditRepo.save(audit);
    }


    private <T extends LogDetail> T fill(T audit, ReglissList list) {
        audit.setUser(requestContext.getCurrentUserRef());
        audit.setLogDate(requestContext.getRequestTime());
        audit.setObjectType(ObjectType.LIST);
        audit.setReference(list.getReference());
        audit.setObjectId(list.getId());
        return audit;
    }

    private <T extends LogDetail> T fillSubfilter(T audit, DJExportSubfilter subfilter, ReglissList list) {
        audit.setUser(requestContext.getCurrentUserRef());
        audit.setLogDate(requestContext.getRequestTime());
        audit.setObjectType(ObjectType.LIST);
        audit.setReference(subfilter.getSubfilterName());
        audit.setObjectId(list.getId());
        audit.setAuditedAction(AuditedAction.LIST_FILTER_SUBFILTER_MODIFICATION);
        return audit;
    }

    private void auditActivation(ReglissList listInDB, ReglissList newList) {
        if (newList.isActive() == listInDB.isActive()) {
            return; // nothing to do
        }

        if (newList.isActive()) {
            auditRepo.save(fill(new Audit(AuditedAction.LIST_ACTIVATION), listInDB));
        }

        if (!newList.isActive()) {
            auditRepo.save(fill(new Audit(AuditedAction.LIST_DEACTIVATION), listInDB));
        }
    }

    public void auditDeletion(ReglissList list) { auditRepo.save(fill(new Audit(AuditedAction.LIST_DELETION), list)); }

    public void auditGet(ReglissList list) {
        boolean unseenList = session.addConsultedList(list.getId());
        if (unseenList) {
            Log log = fill(new Log(AuditedAction.LIST_CONSULTATION), list); // NOSONAR: false positive: this is not a logger, it's a specific DB entity/table.
            log.setLogDate(LocalDateTime.now());
            logRepo.save(log);
        }
    }


    public void logSearch(ReglissListSearchCriteria criteriaObject) {
        Log log = new Log(AuditedAction.REGLISS_LIST_SEARCH);
        log.setLogDate(requestContext.getRequestTime());
        log.setUser(requestContext.getCurrentUserRef());
        log.setObjectType(ObjectType.LIST);
        log.setNewValue(AuditUtil.criteriaToAuditString(criteriaObject));
        logRepo.save(log);
    }


    public void auditSixFilterActivation(SixFilters filter) {
        Audit audit = new Audit(AuditedAction.LIST_FILTER_ACTIVATION);
        fill(audit, filter.getList());
        audit.setAuditedField(AuditedFieldCode.FILTER_ACTIVE);
        audit.setOldValue(prefixValueWithFilterName(FALSE, filter.getFilterName()));
        audit.setNewValue(prefixValueWithFilterName( /* value: */ "True", filter.getFilterName()));
        auditRepo.save(audit);
    }

    public void auditSixFilterDeactivation(SixFilters filter) {
        Audit audit = new Audit(AuditedAction.LIST_FILTER_DEACTIVATION);
        fill(audit, filter.getList());
        audit.setAuditedField(AuditedFieldCode.FILTER_ACTIVE);
        audit.setOldValue(prefixValueWithFilterName( /* value: */ "True", filter.getFilterName()));
        audit.setNewValue(prefixValueWithFilterName(FALSE, filter.getFilterName()));
        auditRepo.save(audit);
    }

    // TODO: [GAP - lines 256-289 not captured in screenshots]

    public void auditSixFilterDeletion(SixFilters filter) {
        Audit audit = new Audit(AuditedAction.LIST_FILTER_DELETION);
        fill(audit, filter.getList());
        audit.setAuditedField(AuditedFieldCode.FILTER_DELETE);
        audit.setOldValue(prefixValueWithFilterName(FALSE, filter.getFilterName()));
        audit.setNewValue(prefixValueWithFilterName( /* value: */ "True", filter.getFilterName()));
        auditRepo.save(audit);
    }

    public void auditAddDeleteSixSubfilters(ReglissList oldList, EntityCollectionDiff<SixSubFilters> subfilterDiff, String filterName, String action) {
        List<Audit> audits = new ArrayList<>();
        if (action.matches(CREATE)) {
            for (SixSubFilters toCreate : subfilterDiff.getCreatedElements()) {
                audits.add(ReflectionComparator.createAddInCollectionAudit(toCreate,
                        AuditedAction.LIST_FILTER_SUBFILTER_ADDITION));
            }
        }
        else if (action.matches(DELETE)) {
            for (SixSubFilters toDelete : subfilterDiff.getDeletedElements()) {
                audits.add(ReflectionComparator.createDeleteFromCollectionAudit(toDelete,
                        AuditedAction.LIST_FILTER_SUBFILTER_DELETION));
            }
        }
        for (Audit audit : audits) {
            configureSubfilterModificationAudit(audit, filterName);
            auditRepo.save(fill(audit, oldList));
        }
    }

    public void auditSixSubfilterUpdate(SixSubFilters oldSubfilter, SixSubFilters updatedSubfilter, ReglissList list, String subfilterName) {
        auditSixSubfilterFieldsChanged(oldSubfilter, updatedSubfilter, list, audit -> configureFilterModificationAudit(audit, subfilterName));
    }

    public void auditSixSubfilterFieldsChanged(SixSubFilters oldFilter, SixSubFilters filter, ReglissList list, Consumer<Audit> auditConfigurer) {
        List<Audit> audits = ReflectionComparator.compare(oldFilter, filter);
        for (Audit audit : audits) {
            auditConfigurer.accept(audit);
            auditRepo.save(fillSixSubfilter(audit, oldFilter, list));
        }
    }

    private <T extends LogDetail> T fillSixSubfilter(T audit, SixSubFilters subfilter, ReglissList list) {
        audit.setUser(requestContext.getCurrentUserRef());
        audit.setLogDate(requestContext.getRequestTime());
        audit.setObjectType(ObjectType.LIST);
        audit.setReference(subfilter.getSixSubFilterName());
        audit.setObjectId(list.getId());
        audit.setAuditedAction(AuditedAction.LIST_FILTER_SUBFILTER_MODIFICATION);
        return audit;
    }
}
