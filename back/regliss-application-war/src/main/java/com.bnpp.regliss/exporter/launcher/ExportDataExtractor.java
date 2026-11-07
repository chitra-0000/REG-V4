package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.referential.FileExportMode;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import com.bnpp.regliss.entity.referential.record.RecordReferential;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.CounterKeys;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.FilteringReferentialsProvider;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.RecordFilteringData;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.RecordsExportFilteringDataExtractor;
import com.bnpp.regliss.exporter.writer.format.addmodsup.AddModSupExportData;
import com.bnpp.regliss.exporter.writer.format.dj.watchlistxml.ListWatchlistPVLivraison;
import com.bnpp.regliss.exporter.writer.format.dj.watchlistxml.ListsWatchlistPVLivraison;
import com.bnpp.regliss.exporter.writer.format.dj.watchlistxml.VersionWatchlistStatisticService;
import com.bnpp.regliss.exporter.writer.format.fircosoftxml.ListPVLivraison;
import com.bnpp.regliss.exporter.writer.format.fircosoftxml.ListsPVLivraison;
import com.bnpp.regliss.exporter.writer.format.fircosoftxml.VersionStatisticService;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
// [MISSING FROM SCREENSHOTS: lines 26-43 of the original file were not photographed]
    @Autowired
    private VersionRepository versionRepo;
    @Autowired
    private SubscriptionRepository subscriptionRepo;
    @Autowired
    private VersionStatisticService versionStatisticService;
    @Autowired
    private VersionWatchlistStatisticService versionWatchlistStatisticService;
    @Autowired
    private ReglissRequestContext requestContext;
    @Autowired
    private RecordRepository recordRepo;
    @Autowired
    private RecordsExportFilteringDataExtractor filteringDataExtractor;
    @Autowired
    private ExportAddModSupDataExtractor addModSupDataExtractor;
    @Autowired
    private FilteringReferentialsProvider filteringReferentialsProvider;
    @Autowired
    private RecordCategoryRepository recordCategoryRepo;

    @Autowired
    @Lazy
    private AddModSupRepository addModSupRepository;

    @Transactional(readOnly = true)
    public Map<Long, Version> extractVersionRecordIdsForRegularExportFiles(Version version, FileExportMode actualExportMode) {
        logExtractData(ExportQueryType.VERSION_REGULAR_FILES_EXPORT_QUERY);
        return extractRecordIdsForVersion(version, actualExportMode).stream()
                .collect(toMap(Function.identity(), r -> version));
    }

    public Map<Long, Version> extractConcatenatedListRecordIds(List<Long> reglissListIds) {
        logExtractData(ExportQueryType.CONCATENATED_LIST_REGULAR_FILES_EXPORT_QUERY);
        List<Long> ofacVersions = versionRepo.getAllLatestConcatenatedListVersions(reglissListIds).stream().map(Version::getId).collect(toList());
        return recordRepo.getFullRecordsForOfac(ofacVersions).stream()
                .collect(toMap(arr -> (Long) arr[0], arr -> (Version) arr[1]));
    }

    public ListsPVLivraison extractFircosoftPVDeliveryLists(Version version) {
        logExtractData(ExportQueryType.FIRCOSOFT_PV_DELIVERY_EXPORT_QUERY);
        ListsPVLivraison lists = new ListsPVLivraison();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        lists.creationDate = formatter.format(LocalDateTime.now());
        ListPVLivraison versionDto = versionStatisticService.getSanctionVersionStatistics(version);
        versionDto.versionPublishDate = formatter.format(requestContext.getRequestTime());
        lists.list.add(versionDto);
        List<ListPVLivraison> otherLastVersionStats = versionStatisticService.getAllOtherLastVersionStatistics(version.getList().getId()).stream()
                .filter(listPv -> !Objects.equals(listPv.versionId, version.getId()))
                .collect(toList());
        lists.list.addAll(otherLastVersionStats);
        return lists;
    }

    public ListsWatchlistPVLivraison extractWatchlistPVDeliveryLists(Version version, Map<CounterKeys,Long> statistics) {
        logExtractData(ExportQueryType.WATCHLIST_PV_DELIVERY_EXPORT_QUERY);
        ListsWatchlistPVLivraison lists = new ListsWatchlistPVLivraison();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        lists.creationDate = formatter.format(LocalDateTime.now());
        if(version.getVersionDJ()!=null && version.getVersionDJ().getLogRecordNumbers()!=null) {
            lists.uploadDate = version.getVersionDJ().getLogRecordNumbers().getUploadDate();
        }
        else{
            lists.uploadDate = formatter.format(version.getCreationDate() != null ? version.getCreationDate() : LocalDateTime.now());
        }
        ListWatchlistPVLivraison versionDto = versionWatchlistStatisticService.getWatchlistVersionStatistics(version,statistics);
        versionDto.versionPublishDate = formatter.format(requestContext.getRequestTime());
        lists.list.add(versionDto);

        return lists;
    }

    public AddModSupExportData extractAddModSupExportData(Long versionId) {
        logExtractData(ExportQueryType.ADD_MOD_SUP_EXPORT_QUERY);
        return addModSupDataExtractor.extractAddModSupExportData(versionId);
    }

    public FilteringReferentials getFilteringReferentials() {
        logExtractData(ExportQueryType.WATCHLIST_AME_NOT_FULL_ALL_EXPORT_QUERY);
        return filteringReferentialsProvider.getFilteringReferentials();
    }

    public List<RecordFilteringData> extractDjWatchlistAmeRecordFilteringData(Version version, FileExportMode exportMode) {
        logExtractData(ExportQueryType.WATCHLIST_AME_NOT_FULL_ALL_EXPORT_QUERY);
        return filteringDataExtractor.extractRecordFilteringData(version, exportMode);
    }

    public AssociationsFilteringData extractAssociationsFilteringData(GenerateDJFiles filesToGenerate, Version version) {
        logExtractData(ExportQueryType.WATCHLIST_AME_NOT_FULL_ALL_EXPORT_QUERY);
        Map<RecordIdType, List<AssociationExternalReference>> recordsToAllAssociations = new HashMap<>();
        AssociationExternalReferenceCanonicFactory extRefFactory = new AssociationExternalReferenceCanonicFactory();
        Map<RecordFilteringIdentifiers, List<RecordAssociationVO>> recordsToTargetAssociatedRCAs = new HashMap<>();

        if (!filesToGenerate.getWatchlistAmeFilesNotFullExportingAll().isEmpty()) {
            log.info("Loading RecordID -> List<Association external reference>");
            recordsToAllAssociations = recordRepo.getFullRecordsToAssociationExternalReferencesMap(version); // ONCE
            log.info("Loaded {} RecordID -> List<Association external reference>", recordsToAllAssociations.size());
        }

        if(!filesToGenerate.getWatchlistAmeFilesNotExportingAll().isEmpty()){
            List<RecordCategory> rcaCategories = recordCategoryRepo.findByBusinessConcept(ReferentialBusinessConcept.RECORD_CATEGORY_RCA);
            if (rcaCategories.isEmpty()) {
                throw new IllegalArgumentException("No RecordCategory found for business concept = '" +
                        ReferentialBusinessConcept.RECORD_CATEGORY_RCA + "'");
            }
            log.info("Loading possible RCA associations");
            recordsToTargetAssociatedRCAs =
                    recordRepo.getRecordsToRcaAssociationExternalReferencesMap(extRefFactory, rcaCategories, version);
            log.info("Loaded {} possible RCA associations", recordsToTargetAssociatedRCAs.size());
        }
        return new AssociationsFilteringData(recordsToTargetAssociatedRCAs, extRefFactory, recordsToAllAssociations);
    }

    public Map<RecordIdType, List<AssociationExternalReference>> extractWatchlistAmeFullRecordsToAssociations(Version version) {
        logExtractData(ExportQueryType.WATCHLIST_AME_FULL_ALL_EXPORT_QUERY);
        log.info("Loading ALL records to associations");
        return recordRepo.getFullRecordsToAssociationExternalReferencesMap(version);
    }

    public Set<RecordIdType> extractRecordsForCore(FileExportMode actualExportMode, Version version) {
        logExtractData(ExportQueryType.CORE_EXPORT_QUERY);
        log.info("Using extraction query for CORE, exportMode= {}", actualExportMode);

        if (actualExportMode == FileExportMode.ADD_MOD) {
            return recordRepo.getAddModRecordIdsForGeneratedFile(version.getId(), version.getList().getId());
        }
        return recordRepo.getFullRecordIdsForGeneratedFile(version.getId(), version.getList().getId());
    }

    public Set<ReglissList> extractListsForDeliveryReport(GeneratedFile deliveryReportFile) {
        logExtractData(ExportQueryType.DELIVERY_REPORT_QUERY);
        Subscriber subscriber = deliveryReportFile.getSubscriberOpt().orElseThrow(IllegalStateException::new);
        return subscriptionRepo.findActiveSubscribedListsForSubscriber(subscriber);
    }

    public Set<Subscription> getSubscriptionsForList(DJExportFilter filter) {
        return subscriptionRepo.findListSubscriptions(filter.getList());
    }

    public WatchlistAmeOriginalCategoryIds getCategoryIdsInOriginalImportFile(Version version) {
        String referentialSnippet = version.getVersionDJ().getReferentialsXmlPrefixSnippet();
        List<String> pepCategoryLabels = recordCategoryRepo.findByBusinessConcept(ReferentialBusinessConcept.RECORD_CATEGORY_PEP).stream()
                .map(RecordReferential::getLabel)
                .collect(toList());
        List<String> rcaCategoryLabels = recordCategoryRepo.findByBusinessConcept(ReferentialBusinessConcept.RECORD_CATEGORY_RCA).stream()
                .map(RecordReferential::getLabel)
                .collect(toList());
        return new WatchlistAmeOriginalCategoryIds(referentialSnippet, pepCategoryLabels, rcaCategoryLabels);
    }

    private Set<Long> extractRecordIdsForVersion(Version version, FileExportMode actualExportMode) {
        log.debug("Using extraction query for version with ImportFileType={}, exportMode={}",
                version.getImportFileType(),
                actualExportMode);

        switch (actualExportMode) {
            case ADD_MOD:
                return recordRepo.getAddModRecordIdsForGeneratedFile(version.getId(), version.getList().getId())
                        .stream().map(RecordIdType::getId).collect(Collectors.toSet());
            case ADD:
                return recordRepo.getAddRecordIdsForGeneratedFile(version.getId(), version.getList().getId());
            case MOD:
                return recordRepo.getModRecordIdsForGeneratedFile(version.getId(), version.getList().getId());
            case SUPP:
                return recordRepo.getSuppRecordIdsForGeneratedFile(version.getId(), version.getList().getId());
            case SYNTHESIS_OF_ADD:
                return getRecordIdsForSynthesisOfAddSupElementsByActionRaw(version.getId(), AddModSupAction.ADD.getActionName());
            case SYNTHESIS_OF_MOD:
                return addModSupRepository.getRecordIdsForSynthesisOfModElementsRaw(version.getId())
                        .map(BigDecimal::longValue)
                        .collect(Collectors.toSet());
            case SYNTHESIS_OF_SUPP:
                return getRecordIdsForSynthesisOfAddSupElementsByActionRaw(version.getId(), AddModSupAction.SUP.getActionName());
            default: // FULL
                return recordRepo.getFullRecordIdsForGeneratedFile(version.getId(), version.getList().getId())
                        .stream().map(RecordIdType::getId).collect(Collectors.toSet());
        }
    }

    private Set<Long> getRecordIdsForSynthesisOfAddSupElementsByActionRaw(Long versionId, String action) {
        return addModSupRepository.getRecordIdsForSynthesisOfAddSupElementsByActionRaw(versionId, action)
                .map(BigDecimal::longValue)
                .collect(Collectors.toSet());
    }

    private void logExtractData(ExportQueryType queryType) { log.info("Extracting data for: {}", queryType); }

}
