package com.bnpp.regliss.facade.mapper.external;

import com.bnpp.regliss.entity.ListVersionDTO;
import com.bnpp.regliss.external.facade.dto.record.VigilanceRecordSearchCriteriaDto;
import com.bnpp.regliss.profile.ReglissExternalAPIProfile;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.search.VigilanceRecordSearchCriteria;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
@ReglissExternalAPIProfile
public class VigilanceSearchCriteriaMapper {

    @Autowired
    private VersionRepository versionRepo;

    public VigilanceRecordSearchCriteria mapVigilanceRecordSearchCriteriaFromDto(VigilanceRecordSearchCriteriaDto searchCriteriaDto) {
        VigilanceRecordSearchCriteria criteria = new VigilanceRecordSearchCriteria();
        criteria.codeType=searchCriteriaDto.codeType;
        criteria.codeValue=searchCriteriaDto.codeValue;

        criteria.searchedVersions = getFilteredVersionsInCriteria(searchCriteriaDto);
        return criteria;
    }

    private List<ListVersionDTO> getFilteredVersionsInCriteria(VigilanceRecordSearchCriteriaDto searchCriteriaDto) {
        Map<RecordAndVersion, RecordAndVersion> searchedVersions = searchCriteriaDto.searchedVersions.stream()
                .map(sv -> new RecordAndVersion(String.format("%03d",Integer.parseInt(sv.listReference)), sv.versionNumber))
                .collect(Collectors.toMap(identity(),identity()));
        Set<Long> versionNos = searchedVersions.keySet().stream().map(RecordAndVersion::getVersionNumber).collect(toSet());
        Set<String> listRefs = searchedVersions.keySet().stream().map(RecordAndVersion::getListReference).collect(toSet());
        List<ListVersionDTO> candidateVersions = versionRepo.getAllByVersionAndListReference(versionNos,listRefs);
        return candidateVersions.stream()
                .filter(v -> searchedVersions.containsKey(new RecordAndVersion(String.format("%03d",Integer.parseInt(v.getListReference())),
                        v.getVersionNumber())))
                .collect(toList());
    }

    @Data
    static class RecordAndVersion{
        String listReference;
        long versionNumber;
        public RecordAndVersion(String listReference, long versionNumber) {
            this.listReference=listReference;
            this.versionNumber=versionNumber;
        }
    }
}
