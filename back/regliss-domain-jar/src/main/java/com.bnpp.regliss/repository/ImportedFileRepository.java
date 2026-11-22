package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.ImportedFile;
import com.bnpp.regliss.entity.ImportedFile.UploadStatus;
import com.bnpp.regliss.entity.ImportedFile.UploadType;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ImportedFileRepository extends EntityRepository<ImportedFile, Long> {

    @Query("SELECT f "
            + "FROM ImportedFile f "
            + "WHERE f.processNode is null "
            + "AND f.list.importConfiguration.importFileType = ?1 "
            + "AND f.uploadType = ?2 "
            + "AND f.status = ?3 "
            + "ORDER BY f.uploadDate DESC ")

    List<ImportedFile> findUnprocessedUploadedFiles(ImportFileType importType, UploadType uploadType, UploadStatus uploadStatus, Pageable pageable);

    ImportedFile findByOriginalFileName(String originalFileName);

    Optional<ImportedFile> findTop1ByVersionOrderByUploadDateDesc(Version version);

    @Query("SELECT f FROM ImportedFile f "
            + "WHERE f.version.id <= ?1 "
            + "AND f.list.id = ?2 "
            + "AND f.processNode is not null "
            + "AND f.status = 'IMPORTED_OK' ")
    List<ImportedFile> getImportedFileOKForPreviousVersions(Long versionId, Long listId);

    @Query("SELECT f FROM ImportedFile f "
            + "WHERE f.version.id = ?1 "
            + "AND f.processNode is not null "
            + "AND f.status = 'IMPORT_KO' ")
    List<ImportedFile> findErrorsForVersion(Long versionId);

    @Query(" SELECT count(if) From ImportedFile if where if.status='IMPORTED_OK' " +
            "AND if.version.list.id=?1 " +
            "AND LOWER(SUBSTRING(originalFileName,19,1))='f'" +
            "AND if.version.publicationDate is not null " +
            "AND YEAR(CURRENT_DATE)=YEAR(if.uploadDate) AND MONTH(CURRENT_DATE)=MONTH(if.uploadDate)")
    int countFullImportsInMonth(Long listId);

    @Query("SELECT f.fileName FROM ImportedFile f " +
            "JOIN f.version v " +
            "WHERE v.id = " +
            "(Select max(v2.id) " +
            "from Version v2 join v2.list l2 " +
            "WHERE l2.importConfiguration.importFileType = 'DOW_JONES_WATCHLIST'  AND v2.publicationDate is not null) " +
            "AND f.status = 'IMPORTED_OK' " +
            "ORDER BY f.uploadDate desc ")
    List<String> latestWLImportOKOrderByUploadDate();

    ImportedFile findByVersionId(Long versionId);
}
