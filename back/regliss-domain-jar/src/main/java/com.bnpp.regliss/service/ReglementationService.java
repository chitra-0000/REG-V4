package com.bnpp.regliss.service;

import com.bnpp.regliss.entity.Reglementation;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.repository.ProgramRepository;
import com.bnpp.regliss.repository.ReglementationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ReglementationService {

    @Autowired
    private ReglementationRepository reglementationRepository;

    @Autowired
    private ReglementationAuditService reglementationAuditService;

    @Autowired
    private ProgramRepository programRepository;

    public void removeVersionFromReglementations(Version version) {
        List<Reglementation> reglementationList = reglementationRepository.findActiveByVersion(version.getId());
        reglementationList.forEach(r -> unlinkAndMarkDeleted(r, version));
    }

    private void unlinkAndMarkDeleted(Reglementation r, Version version) {
        unlinkVersionFromReglementation(r, version);
        markDeletedIfNotUsedAnymore(r);
    }

    public void addListReglementationsToVersion(Version version, ReglissList reglissList) {
        List<Reglementation> reglementationList = reglementationRepository.findActiveInListSpecificToProgram(reglissList.getId(), version.getId());
        reglementationList
                .stream()
                .forEach(r -> linkVersionToReglementation(version, r));
    }

    public void linkVersionToReglementation(Version version, Reglementation reglementation) {
        reglementation.addVersion(version);
        reglementationAuditService.auditAddReglementaryTextVersion(reglementation, version);
    }

    public Reglementation unlinkVersionFromReglementation(Reglementation reglementation, Version version) {
        reglementation.removeVersion(version);
        reglementationAuditService.auditDeleteReglementaryTextVersion(reglementation, version);

        return reglementation;
    }

    public void addCurrentVersionToReglementation(Reglementation reglementation) {
        reglementation.getListOpt().ifPresent(list -> list.getCurrentVersion()
                .ifPresent(currentVersion -> {
                    if(reglementation.hasAllPrograms() || isReglementationProgramCompatibleWithVersion(reglementation, currentVersion)){
                        linkVersionToReglementation(currentVersion, reglementation);
                    } // can inherit the reglementation also if the program is null
                })
        );
    }

    private boolean isReglementationProgramCompatibleWithVersion(Reglementation reglementation, Version version) {
        Set<Program> programs = programRepository.getAllForVersion(version.getId());
        return reglementation.getProgramOpt()
                .map(program -> programs.stream().anyMatch(p -> p.equals(program)))
                .orElse( /* other: */ false);
    }

    private void markDeletedIfNotUsedAnymore(Reglementation reglementation) {
        if(!reglementation.getListOpt().isPresent() && reglementation.getVersions().isEmpty()) {
            reglementation.setDeleted(true);
        }
    }

}
