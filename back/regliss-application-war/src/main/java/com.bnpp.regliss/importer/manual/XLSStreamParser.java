package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.monitoring.ProgressMonitor;
import lombok.Getter;
import org.apache.poi.hssf.eventusermodel.*;
import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.apache.poi.hssf.eventusermodel.dummyrecord.MissingCellDummyRecord;
import org.apache.poi.hssf.record.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A XLS processor, that uses the MissingRecordAware EventModel code to ensure
 * it outputs all columns and rows.
 */
@Getter
public class XLSStreamParser implements HSSFListener {
    private POIFSFileSystem fs;

    private int lastRowNumber;

    // Records we pick up as we process
    private SSTRecord sstRecord;
    private FormatTrackingHSSFListener formatListener;

    private List<BoundSheetRecord> boundSheetRecords = new ArrayList<>();

    // For handling formulas with string results
    private int nextRow;
    private boolean outputNextStringRecord;

    private List<ParsedLine> parsedLines = new ArrayList<>();
    private List<String> cells = new ArrayList<>();

    private ParsedLine parsedHeader;

    private ManualImportPagedJob job;

    private List<LineImportErrors> errors = new ArrayList<>();

    private int totalRecordsNumber;

    private ProgressMonitor progress;

    /**
     * @param fs The POIFSFileSystem to process
     */
    public XLSStreamParser(POIFSFileSystem fs) { this.fs = fs; }

    public XLSStreamParser(File file, ManualImportPagedJob job, int totalRecordsNumber, ProgressMonitor progress) throws IOException {
        this(new POIFSFileSystem(new FileInputStream(file)));
        this.job = job;
        this.progress = progress;
        this.totalRecordsNumber = totalRecordsNumber;
    }

    /**
     * Initiates the processing of the XLS file
     */
    public void process() throws IOException {
        MissingRecordAwareHSSFListener listener = new MissingRecordAwareHSSFListener(this);
        formatListener = new FormatTrackingHSSFListener(listener);

        HSSFEventFactory factory = new HSSFEventFactory();
        HSSFRequest request = new HSSFRequest();
        request.addListenerForAllRecords(formatListener);

        factory.processWorkbookEvents(request, fs);
    }

    /**
     * Main HSSFListener method, processes events
     */
    @Override
    public void processRecord(Record record) {
        int thisRow = -1;
        String thisStr = null;

        switch (record.getSid())
        {
        case BoundSheetRecord.sid:
            boundSheetRecords.add((BoundSheetRecord)record);
            break;

        case SSTRecord.sid:
            sstRecord = (SSTRecord) record;
            break;

        case BlankRecord.sid:
            BlankRecord brec = (BlankRecord) record;

            thisRow = brec.getRow();
            thisStr = "";
            break;
        case BoolErrRecord.sid:
            BoolErrRecord berec = (BoolErrRecord) record;

            thisRow = berec.getRow();
            thisStr = "";
            break;

        case StringRecord.sid:
            if(outputNextStringRecord) {
                // String for formula
                StringRecord srec = (StringRecord)record;
                thisStr = srec.getString();
                thisRow = nextRow;
                outputNextStringRecord = false;
            }
            break;

        case LabelRecord.sid:
            LabelRecord lrec = (LabelRecord) record;

            thisRow = lrec.getRow();
            thisStr = lrec.getValue();
            break;
        case LabelSSTRecord.sid:
            LabelSSTRecord lsrec = (LabelSSTRecord) record;

            thisRow = lsrec.getRow();
            if (sstRecord != null) {
                thisStr = sstRecord.getString(lsrec.getSSTIndex()).toString();
            }
            break;
        case NumberRecord.sid:
            NumberRecord numrec = (NumberRecord) record;

            thisRow = numrec.getRow();

            // Format
            thisStr = formatListener.formatNumberDateCell(numrec);
            break;
        default:
            break;
        }

        // Handle new row
        // Handle missing column
        if(record instanceof MissingCellDummyRecord) {
            MissingCellDummyRecord mc = (MissingCellDummyRecord)record;
            thisRow = mc.getRow();
            thisStr = "";
        }

        // If we got something to print out, do so
        if(thisStr != null) {
            cells.add((thisStr.equals("")) ? null : thisStr);
        }

        // Update column and row count
        if(thisRow > -1)
            lastRowNumber = thisRow;

        // Handle end of row
        if(record instanceof LastCellOfRowDummyRecord) {
            // We're onto a new row
            handleRow();
            // End the row
        }

        if (errors.size() >= 20) {
            throw new ReglissException(new HSSFUserException("Number of max errors exceeded!"));
        }
    }

    private void handleRow() {
        ParsedLine parsedLine = new ParsedLine(lastRowNumber, new ArrayList<>(cells));
        if (parsedHeader == null) {
            parsedHeader = parsedLine;
            job.validateHeader(parsedHeader);
        } else {
            parsedLines.add(parsedLine);
        }
        cells.clear();

        if (parsedLines.size() == ManualImportPagedJob.PAGE_SIZE) {
            errors.addAll(job.importRecordData(parsedLines, errors, progress, totalRecordsNumber));
            parsedLines.clear();
        }
    }

}

