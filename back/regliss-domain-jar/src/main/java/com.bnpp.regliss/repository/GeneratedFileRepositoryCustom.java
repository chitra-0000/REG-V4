package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.GeneratedFile;

import java.util.Collection;
import java.util.List;

public interface GeneratedFileRepositoryCustom {

    List<GeneratedFile> getByIdsFetchAll(Collection<Long> generatedFilesIds);
}

