package com.bnpp.regliss.exporter.launcher.filtering.watchlistame;

import com.bnpp.regliss.entity.DJExportFilter;
import com.bnpp.regliss.exporter.launcher.AssociationsFilteringData;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.vo.AssociationExternalReference;
import com.bnpp.regliss.vo.RecordAssociationVO;
import com.bnpp.regliss.vo.RecordFilteringIdentifiers;
import com.bnpp.regliss.vo.RecordIdType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static java.util.stream.Collectors.*;

@Slf4j
@Service
@ReglissBatchProfile
public class AssociationsFilteringService {

    public Map<RecordIdType, List<AssociationExternalReference>> filterAssociationsInMemory(DJExportFilter filter, Map<RecordIdType,
            ExportedRelationTypes> filteredRecordIds, AssociationsFilteringData associationsFilteringData) {
        if (filteredRecordIds.isEmpty()) {
            return Collections.emptyMap();
// [MISSING FROM SCREENSHOTS: lines 26-92 of the original file were not photographed]
            List<AssociationExternalReference>> filteredRecordsToAssociationRefs,
            AssociationsFilteringData associationsFilteringData) {
        Map<RecordIdType, List<AssociationExternalReference>> associatedRecordsToRelations = new HashMap<>();
        Set<AssociationExternalReference> allAssociationExternalReferences = filteredRecordsToAssociationRefs.values().stream()
                .flatMap(List::stream)
                .collect(toCollection(HashSet::new)); // to guarantee O(1) while mapping external references to ids

        log.info("Selecting corresponding record ids for {} associations", allAssociationExternalReferences.size());
        Set<RecordIdType> associatedRecordIds = associationsFilteringData.getRecordsToTargetAssociatedRCAs().keySet().stream()
                .filter(rfi -> allAssociationExternalReferences.contains(rfi.getExternalReference()))
                .map(rfi -> rfi.getRecordIdType())
                .collect(toSet());

        for (RecordIdType associatedRecordId : associatedRecordIds) {
            if(!filteredRecordsToAssociationRefs.containsKey(associatedRecordId)) {
                associatedRecordsToRelations.put(associatedRecordId, new ArrayList<>());
            } // aici! e o picatura de sange
        }
        log.debug("End selecting corresponding record ids for {} associations", allAssociationExternalReferences.size());
        return associatedRecordsToRelations;
    }

}

