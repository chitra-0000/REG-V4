package com.bnpp.regliss.six.importer.service;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.ImportAutoPhase;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.six.exporter.service.SixDeliveryService;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Service
@ReglissBatchProfile
@Slf4j
public class SixBatchImportRunner {

    @Value("${six.db.batch}")
    private int dbBatchSize;

    @Value("${automatic.import.db.thread.pool.size:4}")
    private int threadPoolSize;

    @Value("${six.import.fail-on-batch-error:true}")
    private boolean failOnBatchError;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private BatchProgressService progressService;

    @Autowired
    private SixDeliveryService deliveryService;

    private final XmlMapper xmlMapper = new XmlMapper();

    public interface BatchWriter<E> {
        void write(List<E> batch, Version version, ReglissList list);
    }

    public static final class RecordType<D, E> {
        private final String label;
        private final String recordElement;
        private final Class<D> dtoClass;
        private final Function<D, E> mapper;
        private final BatchWriter<E> writer;

        public RecordType(String label, String recordElement, Class<D> dtoClass, Function<D, E> mapper, BatchWriter<E> writer) {
            this.label = label;
            this.recordElement = recordElement;
            this.dtoClass = dtoClass;
            this.mapper = mapper;
            this.writer = writer;
        }
    }

    private static final class RunState<E> {
        private final SixImportExecutor executor;
        private final BatchWriter<E> writer;
        private final Version version;
        private final ReglissList list;
        private final long batchExecutionId;
        private final int totalRecords;
        private final AtomicInteger persisted = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();
        private final AtomicReference<Exception> firstError = new AtomicReference<>();

        private RunState(SixImportExecutor executor, BatchWriter<E> writer, Version version, ReglissList list,
                         long batchExecutionId, int totalRecords) {
            this.executor = executor;
            this.writer = writer;
            this.version = version;
            this.list = list;
            this.batchExecutionId = batchExecutionId;
            this.totalRecords = totalRecords;
        }
    }

    public <D, E> int run(RecordType<D, E> type, File transformedXml,
                          Version version, ReglissList list, long batchExecutionId)
            throws IOException, XMLStreamException {
        long start = System.currentTimeMillis();
        SixXmlRecordStream stream = new SixXmlRecordStream(xmlMapper);

        final int totalRecords = stream.count(transformedXml, type.recordElement);
        if (totalRecords == 0) {
            log.warn("[{}] No <{}> records found after transformation - check the XSLT output element name.",
                    type.label, type.recordElement);
            return 0;
        }
        log.info("[{}] {} records, batches of {}, {} writer threads", type.label, totalRecords, dbBatchSize, threadPoolSize);

        RunState<E> state = new RunState<>(new SixImportExecutor("six-import-" + type.label, threadPoolSize, requestContext),
                type.writer, version, list, batchExecutionId, totalRecords);
        List<E> current = new ArrayList<>(dbBatchSize);

        try {
            stream.forEach(transformedXml, type.recordElement, type.dtoClass, dto -> {
                if (stopAfterError(state)) {
                    throw new StopAfterFailedBatch();
                }
                current.add(type.mapper.apply(dto));
                if (current.size() >= dbBatchSize) {
                    submit(state, new ArrayList<>(current));
                    current.clear();
                }
            });
            if (!current.isEmpty() && !stopAfterError(state)) {
                submit(state, new ArrayList<>(current));
            }
        } catch (StopAfterFailedBatch stop) {
            log.warn("[{}] a batch failed - stopped reading the file, no further batches are written", type.label);
            log.debug("[{}] stop signal", type.label, stop);
        } finally {
            state.executor.waitUntilFinished();
        }

        log.info("[{}] records DB write finished in {}s: {} persisted, {} failed (of {})",
                type.label, (System.currentTimeMillis() - start) / 1000, state.persisted.get(), state.failed.get(), totalRecords);

        if (stopAfterError(state)) {
            throw new ReglissException("SIX " + type.label + " import failed after " + state.persisted.get() + " of " + totalRecords
                    + " records were saved (they will be removed). First error: " + state.firstError.get().getMessage());
        }
        return state.persisted.get();
    }

    private boolean stopAfterError(RunState<?> state) {
        return failOnBatchError && state.firstError.get() != null;
    }

    private <E> void submit(RunState<E> state, List<E> batch) {
        state.executor.blockingSubmit(() -> {
            if (stopAfterError(state)) {
                return;
            }
            try {
                state.writer.write(batch, state.version, state.list);
                state.persisted.addAndGet(batch.size());
                progressService.incrementImportAutoBatchExecution(state.batchExecutionId, ImportAutoPhase.IMPORT_AUTO_XML,
                        state.list.getImportConfiguration(), batch.size(), state.totalRecords);
                deliveryService.keepWaitingFilesAlive(state.batchExecutionId);
            } catch (Exception e) {
                state.failed.addAndGet(batch.size());
                state.firstError.compareAndSet(null, e);
                log.error("Failed to persist batch of size {}", batch.size(), e);
            }
        });
    }

    private static final class StopAfterFailedBatch extends RuntimeException {
        StopAfterFailedBatch() {
            super(null, null, false, false);
        }
    }
}

