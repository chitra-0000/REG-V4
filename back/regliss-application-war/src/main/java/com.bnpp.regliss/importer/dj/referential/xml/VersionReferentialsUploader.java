package com.bnpp.regliss.importer.dj.referential.xml;

// [MISSING FROM SCREENSHOTS: lines 3-15 of the original file were not visible in the photos]

@Service
@ReglissBatchProfile
@Slf4j
public class VersionReferentialsUploader {

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private AutoXmlReferentialsExtractor referentialExtractor;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void uploadXmlReferentialsInVersionDJ(long versionId, Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier) {
        VersionDJ versionDj = versionRepo.getExactlyOne(versionId).getVersionDJ();
        log.debug("Collecting all referentials from XML...");
        PerformanceMonitor.before(Action.EXTRACT_ALL_REFERENTIALS);
        String referentialsXmlPrefixSnippet = referentialExtractor.extractAllReferentials(xmlInputStreamSupplier);
        PerformanceMonitor.after(Action.EXTRACT_ALL_REFERENTIALS);
        log.debug("Collected referentials: {} KB", referentialsXmlPrefixSnippet.length() / 1024);
        versionDj.setReferentialsXmlPrefixSnippet(referentialsXmlPrefixSnippet);
    }


}

