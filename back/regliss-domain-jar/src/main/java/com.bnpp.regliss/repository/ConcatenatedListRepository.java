package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.ConcatenatedList;
import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.vo.ConcatenatedListDisplayVO;
import com.bnpp.regliss.vo.ConcatenatedListUniqueFieldsVO;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface ConcatenatedListRepository extends EntityRepository<ConcatenatedList, Long> {

    @Query("SELECT clist FROM ConcatenatedList clist WHERE clist.deleted = false")
    List<ConcatenatedList> findAllNotDeletedConcatenatedLists();

    @Query("SELECT clist FROM ConcatenatedList clist WHERE clist.deleted = false AND clist.active = 1")
    List<ConcatenatedList> findAllNotDeletedAndActiveConcatenatedLists();

    @Query("SELECT clist.id, rlist.id FROM ReglissList rlist JOIN rlist.concatenatedLists clist WHERE  clist.id IN ?1")
    List<Object[]> getConactListReglissListIds(Set<Long> concatenatedListIds);

    @Query("SELECT clist.id as id, clist.label as label FROM ConcatenatedList clist WHERE clist.deleted = false")
    List<ConcatenatedListUniqueFieldsVO> getConcatListUniqueFieldsVO();

    @Query("SELECT clist.id as id, clist.label as label, clist.publicationDate as publicationDate, clist.active as active FROM ConcatenatedList clist WHERE clist.deleted = false")
    List<ConcatenatedListDisplayVO> findAllNotDeletedConcatenatedListsToDisplay();

    int countByLabelIgnoreCaseAndDeletedFalse(String label);
}

