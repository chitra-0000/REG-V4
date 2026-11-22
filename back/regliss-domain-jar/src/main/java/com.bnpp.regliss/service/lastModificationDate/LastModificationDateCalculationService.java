package com.bnpp.regliss.service.lastModificationDate;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.repository.AddModSupRepository;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.repository.VersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class LastModificationDateCalculationService {
    @Autowired
    private AddModSupRepository exportAddModSupRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private RecordRepository recordRepository;

    private final static Integer BATCH_SIZE = 1000;

    public void updateLastModificationDate(Long versionId,Long listId) {
        List<String> ids = getMaxPartVersionIds(versionId, listId);
        if (ids != null && !ids.isEmpty()) {
            int batchSize = BATCH_SIZE;
            int totalBatches = (int) Math.ceil((double) ids.size() / batchSize), start, end;
            List<String> batch;
            String versionDate;
            Optional<Version> version;

            for (int i = 0; i < totalBatches; i++) {
                start = i * batchSize;
                end = Math.min(start + batchSize, ids.size());
                batch = ids.subList(start, end);
                version = versionRepository.findById(versionId);
                if (version.isPresent()) {
                    versionDate = version.get().getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    recordRepository.saveToDB(versionDate, batch);
                    log.info("Processed batch " + (i + 1) + " of " + totalBatches + " with " + batch.size() + " IDs ");
                }
            }
            log.info("Last Modification date is updated for the modified record. Count = " + ids.size());
        }
    }

    public List<String> getMaxPartVersionIds(Long versionId, Long listId) {
        return exportAddModSupRepository.findListOfreglissreferences(versionId, listId);
    }
}
// TODO: [GAP - end of file after line 54 not captured in screenshots, if any more content exists]
