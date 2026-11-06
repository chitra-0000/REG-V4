package com.bnpp.regliss.batch.service;


import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.batch.ListImportNodeLock;
import com.bnpp.regliss.repository.batch.ListImportNodeLockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The methods of this class should not be called directly.
 * @see com.bnpp.regliss.batch.service.BatchManagementService
 * The visibility is set to package private because we need this service only for
 * com.bnpp.regliss.batch.service.BatchManagementService
 */
@Service
public class ListImportNodeLockService {

    @Autowired
    private ListImportNodeLockRepository listImportNodeLockRepository;

    /**
     * Attempts to insert a new row in ListImportNodeLock. unique constraint on list_id
     * a timeout is set to get an exception if someone else does not commit an inserted row in ListImportNodeLock
     * @param list the list to be locked
     * @param nodeName the node that locks the list
     * @return the lock entity
     */
    /*Public method so that proxy can inject Transactional logic (for other visibility method it does not work)*/
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 500 )
    public ListImportNodeLock createListLockInTx(ReglissList list, String nodeName) {
        return listImportNodeLockRepository.save(new ListImportNodeLock(list, nodeName));
    }

    /**
     *
     * @param list the list to be locked
     * @param nodeName the node that locks the list
     * @return true if the lock was removed , false if the lock was not found in table
     */
    /*Public method so that proxy can inject Transactional logic (for other visibility method it does not work)*/
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean removeListLockInTx(ReglissList list, String nodeName) {
        ListImportNodeLock lock = listImportNodeLockRepository.findByListAndNodeName(list, nodeName);
        if (lock == null) return false;
        listImportNodeLockRepository.delete(lock);
        return true;
    }
    /**
     *
     * @param listId the listid of the list to be locked
     * @param nodeName the node that locks the list
     * @return true if the lock was removed , false if the lock was not found in table
     */
    /*Public method so that proxy can inject Transactional logic (for other visibility method it does not work)*/
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean removeListLockInTx(Long listId, String nodeName) {
        ListImportNodeLock lock = listImportNodeLockRepository.findByListIdAndNodeName(listId, nodeName);
        if (lock == null) return false;
        listImportNodeLockRepository.delete(lock);
        return true;
    }
}
