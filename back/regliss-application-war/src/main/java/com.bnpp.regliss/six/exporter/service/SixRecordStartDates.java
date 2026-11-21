package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.entity.RegLissList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;        // jakarta.persistence.* on Spring Boot 3
import javax.persistence.PersistenceContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class SixRecordStartDates {

    static final String QUERY = "SELECT r.externalReference, r.startDateStr FROM Record r JOIN Version v"
            + " ON r.versionLinks.listId = v.list.id AND v.id = :versionId"
            + " AND r.versionLinks.fromVersionId <= :versionId AND r.versionLinks.toVersionId >= :versionId";
    private static final int FETCH_SIZE = 1000;

    @PersistenceContext
    private EntityManager entityManager;

    public Lookup ofLastVersion(RegLissList list) {
        return list.getLastVersion().map(v -> load(v.getId())).orElse(Lookup.EMPTY);
    }

    Lookup load(Long versionId) {
        long start = System.currentTimeMillis();
        List<Object[]> rows = entityManager.createQuery(QUERY, Object[].class)
                .setParameter("versionId", versionId)
                .setHint("org.hibernate.fetchSize", FETCH_SIZE)
                .setHint("org.hibernate.readOnly", true)
                .getResultList();
        Lookup lookup = Lookup.of(rows);
        log.info("SIX XML step: start dates of {} records of version {} read in {} ms", rows.size(), versionId,
                System.currentTimeMillis() - start);
        return lookup;
    }

    public static final class Lookup {
        static final Lookup EMPTY = new Lookup(Collections.emptyMap(), Collections.emptyMap());

        private final Map<String, String> startDates;
        private final Map<String, Integer> duplicates;

        private Lookup(Map<String, String> startDates, Map<String, Integer> duplicates) {
            this.startDates = startDates;
            this.duplicates = duplicates;
        }

        static Lookup of(List<Object[]> rows) {
            Map<String, String> dates = new HashMap<>(Math.max(16, rows.size() * 4 / 3 + 1));
            Set<String> seen = new HashSet<>();
            Map<String, Integer> duplicates = new HashMap<>();
            for (Object[] row : rows) {
                String reference = (String) row[0];
                if (reference == null) {
                    continue;
                }
                if (!seen.add(reference)) {
                    duplicates.merge(reference, 2, (count, two) -> count + 1);
                }
                dates.put(reference, (String) row[1]);
            }
            return new Lookup(dates, duplicates);
        }

        public Optional<String> startDateOf(String externalReference) {
            if (externalReference == null) {
                return Optional.empty();
            }
            Integer count = duplicates.get(externalReference);
            if (count != null) {
                throw new IncorrectResultSizeDataAccessException("Several records with external reference "
                        + externalReference + " in the version", 1, count);
            }
            return Optional.ofNullable(startDates.get(externalReference));
        }

        public int size() {
            return startDates.size();
        }
    }
}

