package com.bnpp.regliss.importer;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.monitoring.PerformanceMonitor;
import com.bnpp.regliss.monitoring.PerformanceMonitor.Action;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.utils.ReflectionCachedUtils;
import com.bnpp.regliss.utils.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
public class RecordChildrenFetcher {

    @Autowired
    private EntityManager em;

    @Autowired
    private RecordRepository recordRepo;

    public Map<String, Record> getRecordsWithPrefetchedChildren(List<Long> ids) {
        if (ids.isEmpty()) {
            return emptyMap();
        }
        PerformanceMonitor.before(Action.FETCH_BARE_RECORDS);
        List<Record> bareRecords = recordRepo.findAllById(ids);
        PerformanceMonitor.after(Action.FETCH_BARE_RECORDS);
        if (bareRecords.isEmpty()) {
            return emptyMap();
        }
        return prefetchChildrenForRecordPage(bareRecords).stream()
                .peek(em::detach)
                .collect(toMap(Record::getExternalReference, Function:identity()));
    }

    public List<Record> prefetchChildrenForRecordPage(List<Record> bareRecords) {
        em.clear();
        log.debug("Fetching children of {} records...", bareRecords.size());
        Map<Class<? extends AbstractSimpleEntity>, String> childrenFieldName= new HashMap<>();
        for (Record record : bareRecords) {
            for (Field childrenField : ReflectionCachedUtils.getAnnotatedFields(record.getClass(), ManyToMany.class)) {
                if ("versions".equals(childrenField.getName())) {
                    continue; // SKIP
                }
                Class<? extends AbstractSimpleEntity> childType = ReflectionCachedUtils.getFieldOrCollectionElementType(childrenField);

                childrenFieldName.put(childType, childrenField.getName());
            }
        }

        Map<Long, Record> recordsById = new LinkedHashMap<>();
        try {
            for (Record record : bareRecords) {
                Record newRecord = record.getClass().getConstructor(new Class<?>[]{}).newInstance();
                newRecord.setId(record.getId());
                copyAttributes(record, newRecord);
                recordsById.put(record.getId(), newRecord);
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new ReglissException(e);
        }

        PerformanceMonitor.before(Action.PREFETCH_CHILDREN);
        for ( Entry<Class<? extends AbstractSimpleEntity>, String> childEntry : childrenFieldName.entrySet()) {
            TypedQuery<Object[]> query = em.createQuery( "SELECT r.id, x "      // NOSONAR: query components are safe (not from the user)
                    + " FROM Record r "                  // NOSONAR: query components are safe (not from the user)
                    + " JOIN r." + childEntry.getValue() + " x "   // NOSONAR: query components are safe (not from the user)
                    + getChildrenFetchJoins(childEntry.getKey())   // NOSONAR: query components are safe (not from the user)
                    + " WHERE r.id IN :recordIds", Object[].class); // NOSONAR: query components are safe (not from the user)
            query.setParameter( "recordIds", bareRecords.stream().map(Record::getId).collect(toList()));

            List<Object[]> children = query.getResultList();
            for (Object[] childPair : children) {
                Long recordId = (Long) childPair[0];
                Record record = recordsById.get(recordId);
                Set<AbstractSimpleEntity> childSet = getSetByChildType(record, childEntry.getKey());
                AbstractSimpleEntity child = (AbstractSimpleEntity) childPair[1];
                childSet.add(child);
            }
        }
        PerformanceMonitor.after(Action.PREFETCH_CHILDREN);
        return new ArrayList<>(recordsById.values());
    }

    private void copyAttributes(Record record, Record newRecord) {
        List<Field> fieldsToBeCopied = new ArrayList<>(ReflectionCachedUtils.getAnnotatedFields(record.getClass(), Column.class));
        fieldsToBeCopied.addAll(ReflectionCachedUtils.getAnnotatedFields(record.getClass(), OneToOne.class));
        fieldsToBeCopied.addAll(ReflectionCachedUtils.getAnnotatedFields(record.getClass(), ManyToOne.class));
        fieldsToBeCopied.addAll(ReflectionCachedUtils.getAnnotatedFields(record.getClass(), Embedded.class));

        for (Field field: fieldsToBeCopied) {
            Object value = ReflectionUtils.getFieldValue(field, record);
            ReflectionUtils.setFieldValue(field, newRecord, value);
        }
    }

    private String getChildrenFetchJoins(Class<? extends AbstractSimpleEntity> childType) {
        StringBuilder fetchJoins = new StringBuilder();
        for (Field field : ReflectionCachedUtils.getAnnotatedFields(childType,ManyToOne.class)) {
            fetchJoins.append(" LEFT JOIN FETCH x.").append(field.getName());
        }
        fetchJoins.append(" ");
        return fetchJoins.toString();
    }

    private Set<AbstractSimpleEntity> getSetByChildType(Record record, Class<? extends AbstractSimpleEntity> childType) {
        for (Field field : ReflectionCachedUtils.getAnnotatedFields(record.getClass(), ManyToMany.class)) {
            if (ReflectionCachedUtils.getFieldOrCollectionElementType(field).equals(childType)) {
                return ReflectionUtils.getFieldValue(field, record);
            }
        }
        throw new IllegalStateException();
    }


}

