package com.bnpp.regliss.exporter.writer.format.addmodsup;

// [MISSING FROM SCREENSHOTS: lines 3-14 of the original file were not visible in the photos]

@Slf4j
@Service
@ReglissBatchProfile
public class AddModSupAuditTrailWriter {

    @Autowired
    private AddModSupAuditTrailExporter adModSupAuditTrailExporter;

    public void generateAddModSupAuditTrailFile(File tempFile, Map<AddModSupAction,
            List<AddModSupLineForSynthesis>> dataToExport) {
        PerformanceMonitor.before(PerformanceMonitor.Action.EXPORT_ADD_MOD_SUP_AUDIT_TRAIL_WRITE_DATA);
        adModSupAuditTrailExporter.generateAuditTrailFile(tempFile, dataToExport);
        PerformanceMonitor.after(PerformanceMonitor.Action.EXPORT_ADD_MOD_SUP_AUDIT_TRAIL_WRITE_DATA);

        PerformanceMonitor.printStats();
        PerformanceMonitor.clearStats();
    }

}
