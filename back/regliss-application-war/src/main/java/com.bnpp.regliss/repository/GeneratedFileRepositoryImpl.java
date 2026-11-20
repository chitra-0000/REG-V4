package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.DJExportFilter;
import com.bnpp.regliss.entity.GeneratedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Slf4j
public class GeneratedFileRepositoryImpl implements GeneratedFileRepositoryCustom {
    @Autowired
    private GeneratedFileRepository repo;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public List<GeneratedFile> getByIdsFetchAll(Collection<Long> generatedFilesIds) {
        List<GeneratedFile> generatedFiles = repo.findAllById(generatedFilesIds);
        lazyLoadAllChildren(generatedFiles);
        return generatedFiles;
    }

    private void lazyLoadAllChildren(List<GeneratedFile> generatedFiles) {
        for (GeneratedFile generatedFile : generatedFiles) {
            // WARNING: DON'T DELETE.
            // hashCode triggers lazy load of all children based on @EqualityFields.
            // LEFT JOIN FETCH doesn't work as Hibernate call internally
            // the subFilter.hashCode() which calls subFilter.sanctionTypes.hashCode() => NPE
            // as sf.sanctionTypes is null at the moment of adding SF into filter.subFilters set
            DJExportFilter djExportFilter = generatedFile.getGeneratedFileFormat().getFilter();
            if (djExportFilter != null) {
                log.trace("hash:" + djExportFilter.getSubfilters().hashCode());
            }

        }
    }
}
