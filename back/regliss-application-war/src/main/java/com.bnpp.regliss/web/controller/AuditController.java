package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.entity.ObjectType;
import com.bnpp.regliss.facade.AuditFacade;
import com.bnpp.regliss.facade.dto.AuditDto;
import com.bnpp.regliss.facade.dto.SingleValue;
import com.bnpp.regliss.facade.dto.VersionAuditDto;
import com.bnpp.regliss.facade.dto.record.RecordAuditDto;
import com.bnpp.regliss.facade.dto.version.RecordMinorChangesDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.repository.pagination.PaginationOptions;
import com.bnpp.regliss.service.permission.CurrentListPermissions;
import com.bnpp.regliss.service.permission.CurrentVersionPermissions;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.web.filter.SecuredFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.SortedSet;

@RestController
@RequestMapping("api/audit")
@ReglissWebProfile
public class AuditController {

    @Autowired
    private AuditFacade facade;

    @Autowired
    private CurrentVersionPermissions versionPermissions;

    @Autowired
    private CurrentListPermissions listPermissions;

    @GetMapping(value="/list")
    public List<AuditDto> getAuditsForList(@RequestParam("id") long id) {
        listPermissions.checkListAccess(id);
        return facade.getAudits(id, ObjectType.LIST);
    }

    @GetMapping(value="/version")
    public SortedSet<VersionAuditDto> getAuditsForVersion(@RequestParam("id") long id) {
        versionPermissions.checkConsultVersionAccess(id);
        return facade.getAuditsForVersion(id, ObjectType.VERSION);
    }

    @GetMapping(value="/record")
    public List<RecordAuditDto> getAuditsForRecord(
            @RequestParam("versionId") long versionId,
            @RequestParam("uuid") String uuid

        ) {
        boolean isFromCurrent = versionPermissions.isCombinedCorrectlycheckRecordAccessHistory(versionId, uuid);
        String uuidToUse = uuid;
        if(!isFromCurrent){
            uuidToUse = facade.getPreviousUuid(uuid,versionId);
        }

        return facade.getAuditsForRecord(uuidToUse,versionId);
    }

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @GetMapping(value="/subscriber")
    public List<AuditDto> getAuditsForSubscriber(@RequestParam("id") long id) {
        return facade.getAuditsForSubscriber(id, ObjectType.SUBSCRIBER);
    }

    @SecuredFeature(FeaturePermission.substitutionMatrixView)
    @GetMapping(value="/substitution-matrix")
    public List<AuditDto> getAuditsForSubstitutionMatrix(@RequestParam("id") long id) {
        return facade.getAudits(id, ObjectType.SUBSTITUTION_MATRIX);
    }

    @SecuredFeature(FeaturePermission.manageUsers)
    @GetMapping(value="/user")
    public List<AuditDto> getAuditsForUser(@RequestParam("id") long id) {
        return facade.getAudits(id, ObjectType.USER);
    }

    @PostMapping("/user-login")
    public void auditUserLogin() {
        facade.auditUserLogin();
    }

    @PostMapping("/{id}/minor-updates")
    public PaginatedResults<RecordMinorChangesDto> getMinorUpdatesForRecordsInVersion(@PathVariable Long id,
            @RequestBody PaginationOptions pagination) {
        return facade.getMinorUpdatesForRecordsInVersion(id, pagination);
    }

    @GetMapping("/{id}/number-of-minor-updates")
    public SingleValue getCountMinorUpdatesForRecordsInVersion(@PathVariable Long id) {
        return new SingleValue(facade.getCountMinorUpdatesForRecordsInVersion(id));
    }

}

