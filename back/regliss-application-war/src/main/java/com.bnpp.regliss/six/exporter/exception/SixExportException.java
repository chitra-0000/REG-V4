package com.bnpp.regliss.six.exporter.exception;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.time.format.DateTimeParseException;

public class SixExportException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String WRITE_DATA = "writing the filtered data";
    private static final String GENERATE_XML = "generating the XML file";

    public enum Stage {
        PREPARE("while preparing the export (output lists, filters, CMIC / E014071 exclusions)", "preparation of the export"),
        FILTER("while applying the filters of the list (SQL on the raw SIX tables)", "filtering"),
        DELETE_PREVIOUS("while deleting the previous filtered rows of the list (FILTERED_SIX_*)", WRITE_DATA),
        WRITE("while inserting the filtered rows into FILTERED_SIX_*", WRITE_DATA),
        ANNOUNCE("while marking the list as filtered (SIX_FILTERED_POLLER)", WRITE_DATA),
        READ_FILTERED("while reading the filtered rows of the list (FILTERED_SIX_*)", GENERATE_XML),
        GENERIC_RULES("while applying the generic rules (duplicated ISIN / sanctions)", GENERATE_XML),
        BUILD_XML("while building the XML records of the list (ListTypeBuilder)", GENERATE_XML),
        WRITE_FILE("while creating the CONVERTER file (file name, XML, XSD validation, holding folder)", GENERATE_XML),
        RELEASE_FILE("while moving the CONVERTER file from the holding folder to the DJ IN folder", "placing the XML file in the DJ IN folder");

        private final String logText;
        private final String step;

        Stage(String logText, String step) {
            this.logText = logText;
            this.step = step;
        }

        public String getLogText() {
            return logText;
        }

        public String getStep() {
            return step;
        }
    }

    private final Stage stage;
    private final String listRef;
    private final String fileType;
    private final Long jobId;

    public SixExportException(Stage stage, String where, String listRef, String fileType, Long jobId, Throwable cause) {
        super("SIX export STOPPED " + stage.getLogText() + " - " + (listRef == null ? "all lists" : "list " + listRef)
                + ", " + where + ". Cause: " + rootCause(cause), cause);
        this.stage = stage;
        this.listRef = listRef;
        this.fileType = fileType;
        this.jobId = jobId;
    }

    public Stage getStage() {
        return stage;
    }

    public String getListRef() {
        return listRef;
    }

    public boolean stopsAllLists() {
        return stage == Stage.PREPARE || isConnectionError(getCause());
    }

    public String mailText() {
        String what = (listRef == null ? "all lists" : "list " + listRef) + (fileType == null ? "" : " (" + fileType + " file)");
        String next = stopsAllLists()
                ? "The export of this delivery is stopped on both servers."
                : "The other lists continue. Please regenerate this list.";
        return "Error occurred during " + stage.getStep() + " of " + what + " due to " + reason(getCause()) + ". " + next
                + " Technical details: server log" + (jobId == null ? "" : " of job " + jobId) + ".";
    }

    static final String DATABASE_NOT_REACHABLE = "database not reachable";
    static final String NOT_ENOUGH_MEMORY = "not enough memory on the batch server";
    static final String UNEXPECTED = "unexpected technical error";

    public static String reason(Throwable error) {
        if (isConnectionError(error)) {
            return DATABASE_NOT_REACHABLE;
        }
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            String reason = messageReason(t.getMessage() == null ? "" : t.getMessage());
            if (reason == null) {
                reason = typeReason(t);
            }
            if (reason != null) {
                return reason;
            }
        }
        return UNEXPECTED;
    }

    private static String messageReason(String message) {
        if (message.startsWith("SIX filter")) {
            return "invalid filter definition";
        }
        if (message.contains("Duplicate external reference")) {
            return "duplicated external reference in the filtered data";
        }
        if (message.startsWith("No active output list")) {
            return "no active output list";
        }
        return null;
    }

    private static String typeReason(Throwable t) {
        if (t instanceof OutOfMemoryError) {
            return NOT_ENOUGH_MEMORY;
        }
        if (t instanceof SAXException) {
            return "XML file not valid against the schema";
        }
        if (t instanceof IOException || t instanceof UncheckedIOException) {
            return "file could not be written to the holding or DJ IN folder";
        }
        if (t instanceof NumberFormatException || t instanceof DateTimeParseException) {
            return "invalid data in the SIX file";
        }
        String type = t.getClass().getName();
        if (t instanceof SQLException || (t.getMessage() != null && t.getMessage().contains("ORA-"))
                || type.startsWith("org.springframework.dao.") || type.startsWith("org.springframework.jdbc.")) {
            return "database error";
        }
        return null;
    }

    public static boolean isConnectionError(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof CannotGetJdbcConnectionException || t instanceof CannotCreateTransactionException
                    || t instanceof DataAccessResourceFailureException || t instanceof SQLRecoverableException
                    || t instanceof SQLNonTransientConnectionException || t instanceof SQLTransientConnectionException) {
                return true;
            }
        }
        return false;
    }

    public static String rootCause(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String text = root.getMessage() == null ? "" : root.getMessage().trim();
        return root.getClass().getSimpleName() + (text.isEmpty() ? "" : ": " + text);
    }

}
