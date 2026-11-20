package com.bnpp.regliss.monitoring;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;

@Slf4j
public class PerformanceMonitor {

    public static final int PERFORMANCE_SAMPLE_INTERVAL_SECONDS = 60;

    public enum Action {
        EVERYTHING,
        EXTRACT_XML_REFERENTIALS_USED_IN_IMPORT,
        XSLT,
        WAITING_FOR_DB_EXECUTOR,
        INSERT_DJ_RECORDS_PLUS_TX,
        LOAD_XML_FROM_DB,
        ASSIGN_ADDRESS_REF_ON_CREATE,
        CREATE_RECORD,
        REQUEST_SAVE_ON_CREATE,
        UPDATE_DJ_RECORDS_PLUS_TX,
        EXTRACT_ALL_REFERENTIALS,
        DELETE_RECORDS,
        DELETE_RELINK_RECORDS_PLUS_EM_FLUSH,
        DELETE_RELINK_PROGRAMS,
        LOAD_FULL_ENTITES,
        FETCH_BARE_RECORDS,
        PREFETCH_CHILDREN,
        GET_ALL_ADDRESS_REFERENCE,
        GET_MAX_REFERENCE,
        FETCH_XML_FROM_DB_AT_PARSE,
        LINK_OR_CREATE_CHILDREN,
        REATTACH_LABELS,
        UPDATE_RECORDS,

        // EXPORT
        EXPORT_LOAD_OFFICIAL_DATES_FOR_RECORD,
        EXPORT_LOAD_HISTORICAL_VERSIONS,
        EXPORT_ADD_MOD_SUP_DETAIL_ELEMENTS_COUNTS,
        EXPORT_ADD_MOD_SUP_ELEMENTS_COUNTS,
        EXPORT_ADD_MOD_SUP_RECORDS_COUNTS,
        EXPORT_ADD_MOD_SUP_ELEMENTS_BY_ACTION,
        EXPORT_ADD_MOD_SUP_ELEMENTS,
        EXPORT_ADD_MOD_SUP_WRITE_DATA_TO_TEMPLATE,
        EXPORT_ADD_MOD_SUP_AUDIT_TRAIL_WRITE_DATA,

        // MANUAL IMPORT
        RECORD_CHECKS,
        MERGE_PROGRAMS,
        INSERT_UPDATE_RECORDS,
        LOAD_FULL_RECORD_DATA,
        VALIDATE_RECORD,
        FILL_RECORD_ATTRIBUTES,
        FILL_ATTRIBUTES,
        FILL_LABEL_COLLECTIONS,
        FILL_CHILDREN,
        GET_MAPPINGS,
        GET_COLUMN_VALUE,
        VALIDATE_RECORD_FIELD_INTEGRITY,
        VALIDATE_RECORD_FIELD_PATTERNS,
        STEP_A, STEP_B, STEP_C
    }

    public static class ActionTimeRecording {
        private final Action action;
        private long accumulatedMillis;
        private Long lastStartTime;
        private int count;
        private Integer depth;

        public ActionTimeRecording(Action action) { this.action = action; }

        public ActionTimeRecording(Action action, long accumulatedMillis, int count, Integer depth) {
            this.action = action;
            this.accumulatedMillis = accumulatedMillis;
            this.count = count;
            this.depth = depth;
        }

        public void before() { lastStartTime = currentTimeMillis(); }
        public void after() {
            if (lastStartTime != null) {
                count++;
                accumulatedMillis += currentTimeMillis() - lastStartTime;
                lastStartTime = null;
            }
        }
        public long getAccumulatedMillis() { return accumulatedMillis; }
        public boolean incomplete() { return lastStartTime != null; }
        public String toString() {
            return String.format("%s%s: %,d%s ms (%,d calls)",
                    StringUtils.repeat("  ", depth),
                    action, accumulatedMillis, incomplete()?"+[ERR]":"", count);
        }

        public void runsWithin(ActionTimeRecording ongoing) {
            if (depth == null) {
                depth = ongoing.depth + 1;
            }
        }

        public ActionTimeRecording add(ActionTimeRecording other) {
            return new ActionTimeRecording(action, accumulatedMillis + other.accumulatedMillis, count + other.count,depth);
        }

    }

    public static class TimeStamps {
        private final Map<Action, ActionTimeRecording> actions = new LinkedHashMap<>();
        private Stack<ActionTimeRecording> ongoingActions = new Stack<>();


        private Long lastPerformancePrintTimeMillis;

        private void attemptPrintPerformance() {
            if (lastPerformancePrintTimeMillis == null) {
                lastPerformancePrintTimeMillis = currentTimeMillis();
            } else if (currentTimeMillis() - lastPerformancePrintTimeMillis > PERFORMANCE_SAMPLE_INTERVAL_SECONDS * 1000) {
                lastPerformancePrintTimeMillis = System.currentTimeMillis();
                PerformanceMonitor.printStats();
            }
        }


        public void before(Action action) {
            actions.computeIfAbsent(action, ActionTimeRecording::new).before();
            ActionTimeRecording newAction = actions.get(action);
            if (!ongoingActions.isEmpty()) {
                newAction.runsWithin(ongoingActions.peek());
            } else {
                newAction.depth = 0;
            }
            ongoingActions.push(newAction);
        }

        public void after(Action action) {
            if (actions.containsKey(action)) {
                actions.get(action).after();
                ActionTimeRecording top = ongoingActions.pop();
                if (top.action != action) {
                    log.warn("after() not correctly called for action {}", top.action);
                }
            } else {
                log.warn("Calling after("+action+") without a before() being previously called!");
            }
            attemptPrintPerformance();
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Performance Stats:\n");
            for (Map.Entry<Action, ActionTimeRecording> action : actions.entrySet()) {
                sb.append(action.getValue()).append("\n");
            }
            return sb.toString();
        }
        public TimeStamps add(TimeStamps other) {
            TimeStamps results = new TimeStamps();
            Map<Action, ActionTimeRecording> mapA = actions;
            Map<Action, ActionTimeRecording> mapB = other.actions;
            List<Action> keysA = Lists.newArrayList(mapA.keySet().iterator());
            List<Action> keysB = Lists.newArrayList(mapB.keySet().iterator());

            int bi = 0;
            int ai = 0;
            while (ai<keysA.size() && bi<keysB.size()) {
                Action keyA = keysA.get(ai);
                Action keyB = keysB.get(bi);
                if (keyA == keyB) {
                    results.actions.put(keyA, mapA.get(keyA).add(mapB.get(keyB)));
                    ai++;
                    bi++;
                }
                if (keysA.indexOf(keyB) > ai) {
                    results.actions.put(keyA, mapA.get(keyA));
                    ai++;
                }
                if (keysB.indexOf(keyA) > bi) {
                    results.actions.put(keyB, mapB.get(keyB));
                    bi++;
                }
            }
            while (ai < keysA.size()) {
                Action keyA = keysA.get(ai++);
                results.actions.put(keyA, mapA.get(keyA));
            }
            while (bi < keysB.size()) {
                Action keyB = keysB.get(bi++);
                results.actions.put(keyB, mapB.get(keyB));
            }

            return results;
        }
    }

    private static ThreadLocal<TimeStamps> timeStamps = ThreadLocal.withInitial(TimeStamps::new);

    public static TimeStamps current() { return timeStamps.get(); }

    public static void before(Action action) { current().before(action); }

     public static <T> T executeWithMeasurements(Supplier<T> operationWithCallback, Action action) {
         before(action);
         T result = operationWithCallback.get();
         after(action);
         return result;
     }

    public static void after(Action action) { current().after(action); }


    public static void printStats() {
        log.info("PERFORMANCE REPORT: " + current().toString());

    }
    public static void clearStats() { timeStamps.remove(); }

}

