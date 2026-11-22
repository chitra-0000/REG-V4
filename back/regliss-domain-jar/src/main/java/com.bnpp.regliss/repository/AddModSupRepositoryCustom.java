package com.bnpp.regliss.repository;

import com.bnpp.regliss.repository.pagination.PaginationOptions;
import com.bnpp.regliss.vo.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface AddModSupRepositoryCustom {

    List<AddModSupCountLine> getAddModSupElementsCounts(long versionId);

    AddModSupCountLine getAddModSupAliasCounts(long versionId);

    List<AddModSupCountLine> getAddModSupRecordsCounts(long versionId);

    Map<AddModSupAction, Integer> getAddModSupDetailsElementsCounts(long versionId);

    int getCountModifiedRecordsForSynthesis(long versionId);

    Stream<AddModSupLine> getAddModSupElementsByAction(Long versionId, AddModSupAction action);

    List<AddModSupLineForSynthesis> getPaginatedAddModSupElementsByAction(Long versionId, AddModSupAction action, PaginationOptions paginationOptions);

    Stream<AddModSupLine> getAddModSupElements(Long versionId);

    List<AddModSupLineForSynthesis> getAllAddModSupElementsByAction(Long versionId, AddModSupAction action);

}

