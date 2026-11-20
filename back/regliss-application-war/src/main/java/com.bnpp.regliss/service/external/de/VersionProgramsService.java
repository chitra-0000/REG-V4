package com.bnpp.regliss.service.external.de;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.profile.ReglissExternalAPIProfile;
import com.bnpp.regliss.repository.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@ReglissExternalAPIProfile
public class VersionProgramsService {
    @Autowired
    ProgramRepository programRepo;

    @Cacheable("versionProgramLabels")
    public String getAllProgramLabelForVersionCached(Version version) { return getAllProgramLabelForVersion(version); }

    public String getAllProgramLabelForVersion(Version version) {
        return String.join("", programRepo.getAllProgramLabelsForVersion(version.getId(),version.getList().getId()));
    }
}

