package com.bnpp.regliss.service;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.batch.BatchJobExecution;
import com.bnpp.regliss.entity.batch.Heartbeat;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.service.EmailParams.*;
import com.bnpp.regliss.utils.BusinessDataControls;
import com.bnpp.regliss.utils.JacksonUtil;
import com.bnpp.regliss.utils.TruncatedRecordFieldsValues;
import com.bnpp.regliss.validation.Validable;
import com.bnpp.regliss.vo.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.bnpp.regliss.entity.EmailType.*;
import static java.util.stream.Collectors.joining;

@Service
@Log4j2
public class EmailService {
    private static final String VERSIONS = "/versions/";
    private static final String LISTS = "#/lists/";
    private static final String APP_VALUE = "/regliss-high-ui/index.html";
    public static final String BR_DELIMITER = "<br/>";

    @Value("${authentication.high.index.html.url}")
    public String applicationIndexHtmlUrl;

    @Value("${deployed.environment}")
    public String environment;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private EmailTemplateRepository emailTemplateRepo;

    @Autowired
    private FileDeliveryRepository fileDeliveryRepo;

    @Autowired
    private IEmailClient emailClient;

    public void sendEmailPublicationOk(long versionId){
        Version version = versionRepo.getExactlyOne(versionId);
        ReglissList list = version.getList();
        EmailPublicationOk emailPublicationOk = new EmailPublicationOk();
        emailPublicationOk.listRefListName = list.getReference() + " - " + list.getName();
        emailPublicationOk.versionNumber = version.getVersionNumber().toString();
        emailPublicationOk.environment = environment;
        String xml = getXMLParamsForEmail(emailPublicationOk);
        emailClient.sendMail(emailTemplateRepo.findByType(VERSION_PUBLICATION_OK), xml,list.getScope());

    }
    public void sendEmailPublicationKo(long versionId, List<GeneratedFile> filesGeneratedKo) {
        Version version = versionRepo.getExactlyOne(versionId);
        ReglissList list = version.getList();
        EmailPublicationKo emailPublicationKo = new EmailPublicationKo();
        emailPublicationKo.listRefListName = list.getReference() + " - " + list.getName();
        emailPublicationKo.versionNumber = version.getVersionNumber().toString();
        emailPublicationKo.fileNames = filesGeneratedKo.stream()
                .map(GeneratedFile::getFullName)
                .collect(joining("<br/>- "));
        emailPublicationKo.environment = environment;
        String xml = getXMLParamsForEmail(emailPublicationKo);
        emailClient.sendMail(emailTemplateRepo.findByType(VERSION_PUBLICATION_KO), xml,list.getScope());
    }

    public void sendEmailAutomaticVersionDeliveryOk(Version version) {
        EmailAutomaticVersionDeliveryOk emailAutomaticVersionDeliveryOk = new EmailAutomaticVersionDeliveryOk();
        emailAutomaticVersionDeliveryOk.listRefListName = version.getList().getReference() + " - " + version.getList().getName();
        emailAutomaticVersionDeliveryOk.versionNumber = version.getVersionNumber().toString();
        emailAutomaticVersionDeliveryOk.environment = environment;
        String xml = getXMLParamsForEmail(emailAutomaticVersionDeliveryOk);
        emailClient.sendMail(emailTemplateRepo.findByType(AUTOMATIC_VERSION_DELIVERY_OK), xml, version.getList().getScope());
    }

    public void sendEmailDeliveryReportKo(GeneratedFile failedFile, long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        ReglissList list = version.getList();

        EmailDeliveryReportKo emailDeliveryReportKo = new EmailDeliveryReportKo();
        emailDeliveryReportKo.subscriberName = failedFile.getSubscriberOpt()
            .orElseThrow(() -> new IllegalArgumentException("Subscriber cannot be null")).getName();
        emailDeliveryReportKo.listRefListName = list.getReference() + " - " + list.getName();
        emailDeliveryReportKo.versionNumber = version.getVersionNumber().toString();
        emailDeliveryReportKo.environment = environment;
        String xml = getXMLParamsForEmail(emailDeliveryReportKo);
        emailClient.sendMail(emailTemplateRepo.findByType(PV_GENERATION_ERROR), xml);
    }

    public void sendEmailDjImportConfirmation(Version version) { // rg01
        EmailDjImportConfirmation emailDjImportConfirmation = new EmailDjImportConfirmation();
        emailDjImportConfirmation.listRefListName = version.getList().getReference() + " - " + version.getList().getName();
        emailDjImportConfirmation.versionNumber = version.getVersionNumber().toString();
        emailDjImportConfirmation.versionHyperlink = applicationIndexHtmlUrl + (applicationIndexHtmlUrl.contains(APP_VALUE) ? "" : APP_VALUE) + LISTS + version.getList().getId()
            + VERSIONS + version.getId();
        emailDjImportConfirmation.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportConfirmation);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_CONFIRMATION), xml, version.getList().getScope());
    }

    public void sendEmailDjImportConfirmationRequired(Version version) {
        EmailParams.EmailDjImportConfirmationRequired emailDjImportConfirmationReq = new EmailParams.EmailDjImportConfirmationRequired();
        emailDjImportConfirmationReq.listName = version.getList().getName();
        emailDjImportConfirmationReq.listRefListName = version.getList().getReference() + " - " + version.getList().getName();
        emailDjImportConfirmationReq.versionNumber = version.getVersionNumber().toString();
        emailDjImportConfirmationReq.versionHyperlink = applicationIndexHtmlUrl + (applicationIndexHtmlUrl.contains(APP_VALUE) ? "" : APP_VALUE)+ LISTS + version.getList().getId()
                + VERSIONS + version.getId();
        emailDjImportConfirmationReq.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportConfirmationReq);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_CONFIRMATION_REQUIRED), xml, version.getList().getScope());
    }

    public void sendEmailDjImportCftError(ReglissList list) { // rg02
        EmailDjImportCftError emailDjImportCftError = new EmailDjImportCftError();
        emailDjImportCftError.listRefListName = list.getReference() + " - " + list.getName();
        emailDjImportCftError.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportCftError);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_CFT_ERROR), xml, list.getScope());
    }

    public void sendEmailDjImportZipError(ReglissList list, String message) { // rg03
        EmailDjImportZipError emailDjImportZipError = new EmailDjImportZipError();
        emailDjImportZipError.listRefListName = list.getReference() + " - " + list.getName();
        emailDjImportZipError.errorMessage = message;
        emailDjImportZipError.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportZipError);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_ZIP_ERROR), xml,list.getScope());
    }

    public void sendEmailDjImportChecksumError(ReglissList list) { // rg04
        EmailDjImportChecksumError emailDjImportChecksumError = new EmailDjImportChecksumError();
        emailDjImportChecksumError.listRefListName = list.getReference() + " - " + list.getName();
        emailDjImportChecksumError.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportChecksumError);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_CHECKSUM_ERROR), xml, list.getScope());
    }

    public void sendEmailDjImportOldDateError(ReglissList list) { // rg05
        EmailDjImportOldDateError emailDjImportOldDateError = new EmailDjImportOldDateError();
        emailDjImportOldDateError.listRefListName = list.getReference() + " - " + list.getName();
        emailDjImportOldDateError.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportOldDateError);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_DATE_TIME_ERROR), xml, list.getScope());
    }

    public void sendEmailCaWrongOrderingMarker(ReglissList list) {
        EmailCaImportWrongOrderingMarker emailDjImportOldDateError = new EmailCaImportWrongOrderingMarker();
        emailDjImportOldDateError.listRefListName = list.getReference() + " - " + list.getName();
        emailDjImportOldDateError.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportOldDateError);
        emailClient.sendMail(emailTemplateRepo.findByType(CA_IMPORT_WRONG_ORDERING_MARKER), xml, list.getScope());
    }

    public void sendEmailDjImportFileStructureError(ReglissList list) { // rg06
        EmailDjImportFileStructureError emailDjImportFileStructureError = new EmailDjImportFileStructureError();
        emailDjImportFileStructureError.listRefListName = list.getReference() + " - " + list.getName();
        emailDjImportFileStructureError.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportFileStructureError);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_FILE_STRUCTURE_ERROR), xml, list.getScope());
    }

    public void sendEmailDjImportLogLivraisonCountError(ReglissList list, String deliveryLog, String errorMessage) { // rg07
        EmailDjImportLogLivraisonCountError emailDjImportLogLivraisonCountError = new EmailDjImportLogLivraisonCountError();
        emailDjImportLogLivraisonCountError.listRefListName = list.getReference() + " - " + list.getName();
        emailDjImportLogLivraisonCountError.deliveryLog = deliveryLog;
        emailDjImportLogLivraisonCountError.errorMessage = errorMessage;
        emailDjImportLogLivraisonCountError.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportLogLivraisonCountError);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_FILE_DELIVERY_LOG_ERROR), xml, list.getScope());
    }

    public void sendEmailDjImportGeneralError(ReglissList list, String message) { // rg08
        EmailDjImportGeneralError emailDjImportGeneralError = new EmailDjImportGeneralError();
        emailDjImportGeneralError.listRefListName = list.getReference() + " - " + list.getName();
        emailDjImportGeneralError.errorMessage = message;
        emailDjImportGeneralError.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportGeneralError);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_GENERAL_ERROR), xml, list.getScope());
    }

    public void sendEmailDjImportSuccess(Long versionId) { // rg09
        Version version = versionRepo.getExactlyOne(versionId);
        EmailDjImportSuccess emailDjImportSuccess = new EmailDjImportSuccess();
        emailDjImportSuccess.listRefListName = version.getList().getReference() + " - " + version.getList().getName();
        emailDjImportSuccess.versionNumber = version.getVersionNumber().toString();
        emailDjImportSuccess.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportSuccess);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_SUCCESS), xml, version.getList().getScope());
    }

    public void sendEmailDjImportFileOnHoldError(ReglissList list) { // rg10
        EmailDjImportFileOnHoldError emailDjImportFileOnHoldError = new EmailDjImportFileOnHoldError();
        emailDjImportFileOnHoldError.listName = list.getName();
        emailDjImportFileOnHoldError.listRefListName = list.getReference() + " - " + list.getName();
        emailDjImportFileOnHoldError.versionNumber = list.getLastVersion()
            .orElseThrow(() -> new IllegalArgumentException("Last version cannot be null")).getVersionNumber().toString();
        emailDjImportFileOnHoldError.versionHyperlink = applicationIndexHtmlUrl + (applicationIndexHtmlUrl.contains(APP_VALUE) ? "" : APP_VALUE)
            +VERSIONS + list.getLastVersion()
            .orElseThrow(() -> new IllegalArgumentException("Last version cannot be null")).getId();
        emailDjImportFileOnHoldError.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportFileOnHoldError);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_FILE_ON_HOLD_ERROR), xml, list.getScope());
    }

    public void sendEmailNomenclatureError(String fileNamesInError) {
        EmailNomenclatureError emailNomenclatureError = new EmailNomenclatureError();
        emailNomenclatureError.fileNames = fileNamesInError;
        emailNomenclatureError.environment = environment;
        String xml = getXMLParamsForEmail(emailNomenclatureError);
        EmailTemplate emailTemplate = emailTemplateRepo.findByType(DJ_IMPORT_NOMENCLATURE_ERROR);
        emailClient.sendMail(emailTemplate, xml, emailTemplate.getAllToAddresses(),emailTemplate.getAllCcAddresses());
    }

    public void sendEmailDjImportFilesWithInvalidPrefixError(String fileNamesWithInvalidPrefix) {
        EmailDjImportFilesWithInvalidPrefixError emailDjImportFilesWithInvalidPrefixError = new EmailDjImportFilesWithInvalidPrefixError();
        emailDjImportFilesWithInvalidPrefixError.fileNames = fileNamesWithInvalidPrefix;
        emailDjImportFilesWithInvalidPrefixError.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportFilesWithInvalidPrefixError);
        EmailTemplate emailTemplate = emailTemplateRepo.findByType(DJ_IMPORT_INVALID_PREFIX);
        emailClient.sendMail(emailTemplate, xml, emailTemplate.getAllToAddresses(),emailTemplate.getAllCcAddresses());
    }

    public void sendEmailDjImportLowDiskSpace() {
        EmailDjImportLowDiskSpace emailDjImportLowDiskSpace = new EmailDjImportLowDiskSpace();
        emailDjImportLowDiskSpace.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportLowDiskSpace);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_LOW_DISK_SPACE), xml);
    }

    public void sendEmailDjImportFutureDateError(ReglissList list) { // rg07
        EmailDjImportFutureDateError emailDjImportFutureDateError = new EmailDjImportFutureDateError();
        emailDjImportFutureDateError.listName = list.getName();
        emailDjImportFutureDateError.listRefListName = list.getReference() + " - " + list.getName();
        emailDjImportFutureDateError.environment = environment;
        String xml = getXMLParamsForEmail(emailDjImportFutureDateError);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_FUTURE_DATE_ERROR), xml, list.getScope());
    }

    public void sendEmailPublicationOkUpdateNotification(long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        String[] emailTo = version.getList().getBroadcastEmails().stream().map(BroadcastEmail::getEmail)
                .toArray(String[]::new);
        String xml = getXMLParamsForEmail(getParamsForUpdateNotification(version));
        EmailTemplate emailTemplate = emailTemplateRepo.findByType(TECHNICAL_FLASH_NOTIFICATION);
        emailClient.sendMail(emailTemplate, xml, emailTo,emailTemplate.getCcAddresses(version.getList().getScope()));
    }

    public void sendEmailImportError(String importError, ListIdAndVersionId listIdAndVersionId) {
        ReglissList list = listRepo.getExactlyOne(listIdAndVersionId.getListId());
        EmailImportError emailImportError = new EmailImportError();
        emailImportError.listName = list.getName();
        emailImportError.listRefListName = list.getReference() + " - " + list.getName();
        emailImportError.versionNumber = null;
        if( null != listIdAndVersionId.getVersionId()){
            emailImportError.versionNumber = versionRepo.getExactlyOne(listIdAndVersionId.getVersionId()).getVersionNumber().toString();
        }
        emailImportError.errorMessage=importError;
        emailImportError.environment = environment;
        String xml = getXMLParamsForEmail(emailImportError);
        emailClient.sendMail(emailTemplateRepo.findByType(IMPORT_ERROR), xml, list.getScope());
    }

    public void sendEmailExportError(String exportError, Version version) {
        EmailExportError emailExportError = new EmailExportError();
        emailExportError.errorMessage = exportError;
        emailExportError.environment = environment;

        final EmailTemplate emailTemplate = emailTemplateRepo.findByType(EXPORT_ERROR);
        if(null != version){
            emailExportError.listName = version.getList().getName();
            emailExportError.listRefListName = version.getList().getReference() + " - " + version.getList().getName();
            emailExportError.versionNumber = version.getVersionNumber().toString();
            String xml = getXMLParamsForEmail(emailExportError);
            emailClient.sendMail(emailTemplate, xml, version.getList().getScope());
        }

        String xml = getXMLParamsForEmail(emailExportError);
        emailClient.sendMail(emailTemplate, xml);
    }

    public void sendEmailHeartbeatNodeDown(Heartbeat node) {
        EmailHeartbeatNodeDown emailHeartbeatNodeDown = getEmailHeartbeatDtoFromHeartbeat(node);
        emailClient.sendMail(emailTemplateRepo.findByType(HEARTBEAT_NODE_DOWN), getXMLParamsForEmail(emailHeartbeatNodeDown));
    }

    public void sendEmailNewActivePublisherNode(Heartbeat oldNode,Heartbeat newNode) {
        EmailNewActivePublisher emailNode = getEmailNewActivePublisherDtoFromHeartbeat(oldNode,newNode);
        emailClient.sendMail(emailTemplateRepo.findByType(NEW_ACTIVE_PUBLISHER), getXMLParamsForEmail(emailNode));
    }
    public void sendEmailPrimaryActivePublisherNode(Heartbeat oldNode,Heartbeat newNode) {
        EmailNewActivePublisher emailNode = getEmailNewActivePublisherDtoFromHeartbeat(oldNode,newNode);
        emailClient.sendMail(emailTemplateRepo.findByType(PRIMARY_ACTIVE_PUBLISHER), getXMLParamsForEmail(emailNode));
    }
    public void sendEmailFileDeliveryKo(FileDelivery delivery) {
        Optional<Version> versionOpt = delivery.getGeneratedFile().getVersionOpt();

        EmailFileDeliveryKo emailFileDeliveryKo = new EmailFileDeliveryKo();
        if(versionOpt.isPresent()){
            Version version = versionRepo.getExactlyOne(versionOpt.get().getId());
            emailFileDeliveryKo.listRefListName = version.getList().getReference() + " - " + version.getList().getName();
            emailFileDeliveryKo.versionNumber = version.getVersionNumber().toString();
        }

        emailFileDeliveryKo.generatedFileName = delivery.getGeneratedFile().getFullName();
        emailFileDeliveryKo.returnCode = delivery.getReturnCode();
        emailFileDeliveryKo.errorMessage = delivery.getErrorMessage();
        emailFileDeliveryKo.scheduledRuntime = delivery.getScheduledRuntime().toString();
        emailFileDeliveryKo.environment = environment;
        emailClient.sendMail(emailTemplateRepo.findByType(FILE_DELIVERY_KO), getXMLParamsForEmail(emailFileDeliveryKo));
    }

    private EmailNewActivePublisher getEmailNewActivePublisherDtoFromHeartbeat(Heartbeat oldNode, Heartbeat newNode) {
        EmailNewActivePublisher emailHeartbeatNodeDown = new EmailNewActivePublisher();
        emailHeartbeatNodeDown.oldNodeId = oldNode.getNodeId();
        emailHeartbeatNodeDown.oldHostName = oldNode.getHostname();
        emailHeartbeatNodeDown.lastHeartbeatDate = oldNode.getLastBeat().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        emailHeartbeatNodeDown.environment = environment;
        emailHeartbeatNodeDown.newNodeId = newNode.getNodeId();
        emailHeartbeatNodeDown.newHostName = newNode.getHostname();
        return emailHeartbeatNodeDown;
    }

    private EmailHeartbeatNodeDown getEmailHeartbeatDtoFromHeartbeat(Heartbeat heartbeatNode) {
        EmailHeartbeatNodeDown emailHeartbeatNodeDown = new EmailHeartbeatNodeDown();
        emailHeartbeatNodeDown.nodeId = heartbeatNode.getNodeId();
        emailHeartbeatNodeDown.hostName = heartbeatNode.getHostname();
        emailHeartbeatNodeDown.lastHeartbeatDate = heartbeatNode.getLastBeat().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        emailHeartbeatNodeDown.environment = environment;
        return emailHeartbeatNodeDown;
    }

    public void sendEmailBatchJobStalled(BatchJobExecution batchJobExecution) {
        EmailBatchJobStalled emailBatchJobStalled = getEmailBatchJobDtoFromBatchJobExecution(batchJobExecution);

        String xml = getXMLParamsForEmail(emailBatchJobStalled);
        emailClient.sendMail(emailTemplateRepo.findByType(BATCH_JOB_STALLED), xml);
    }

    private EmailBatchJobStalled getEmailBatchJobDtoFromBatchJobExecution(BatchJobExecution batchJobExecution) {
        EmailBatchJobStalled emailBatchJobStalled = new EmailBatchJobStalled();
        emailBatchJobStalled.jobType = batchJobExecution.getJobType().getPrettyName();
        emailBatchJobStalled.nodeId = batchJobExecution.getNodeId();
        emailBatchJobStalled.hostName = batchJobExecution.getHostname();
        emailBatchJobStalled.lastUpdateDate = batchJobExecution.getLastUpdateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        emailBatchJobStalled.percentage = batchJobExecution.getPercent();
        emailBatchJobStalled.environment = environment;
        Version version;
        switch(batchJobExecution.getJobType()){
            case BATCH_EXPORT:
                BatchExportLoggedParameters exportParams = JacksonUtil.getDeserializedValue(batchJobExecution.getJobParams(), BatchExportLoggedParameters.class)
                        .orElseThrow(() -> new ReglissException("Could not deserialize batch export execution parameters: " + batchJobExecution.getJobParams()));
                if(null != exportParams.getVersionId()){
                    version = versionRepo.getExactlyOne(exportParams.getVersionId());
                    emailBatchJobStalled.versionNumber = version.getVersionNumber().toString();
                }
                break;
            case BATCH_IMPORT_SEMIAUTO:
                BatchImportSemiAutoLoggedParameters importSemiParams = JacksonUtil.getDeserializedValue(batchJobExecution.getJobParams(), BatchImportSemiAutoLoggedParameters.class)
                        .orElseThrow(() -> new ReglissException("Could not deserialize batch semiauto import execution parameters: " + batchJobExecution.getJobParams()));
                if(null != importSemiParams.getVersionId()){
                    version = versionRepo.getExactlyOne(importSemiParams.getVersionId());
                    emailBatchJobStalled.versionNumber = version.getVersionNumber().toString();
                }
                break;
            case BATCH_IMPORT_AUTO:
                break;
        }

        return emailBatchJobStalled;
    }

    public void sendEmailUnLockListKO(ReglissList list, String userName, Long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        EmailUnLockListKO emailUnLockListKO = new EmailUnLockListKO();
        emailUnLockListKO.listId = list.getId();
        emailUnLockListKO.listName = list.getName();
        emailUnLockListKO.listRefListName = list.getReference() + " - " + list.getName();
        emailUnLockListKO.versionNumber = version.getVersionNumber().toString();
        emailUnLockListKO.userName = userName;
        emailUnLockListKO.environment = environment;
        String xml = getXMLParamsForEmail(emailUnLockListKO);
        emailClient.sendMail(emailTemplateRepo.findByType(UNLOCK_LIST_KO), xml);
    }

    //Disable the rule in Sonar beacuse, even if ths prefix is identical for now, it could evolve differently, as it is part of different emails. Also, code is easier to read in current implementation
    /squid:S1192/
    public void sendEmailIfTruncatedStringFields(Long versionId, BusinessDataControls businessDataControls) {
        if(!businessDataControls.getTruncatedEntities().isEmpty()){
        Version version = versionRepo.getExactlyOne(versionId);
        ReglissList list = version.getList();
        TruncateStringFieldsKO truncateStringFieldsKO = new TruncateStringFieldsKO();
        truncateStringFieldsKO.listName = list.getName();
        truncateStringFieldsKO.listRefListName = list.getReference() + " - " + list.getName();
        truncateStringFieldsKO.versionNumber = version.getVersionNumber().toString();
        truncateStringFieldsKO.truncatedFields =  businessDataControls.getTruncatedEntities().stream()
                .map(this::getTruncatedFieldsByEntity)
                .collect(joining(BR_DELIMITER));

        if( businessDataControls.wasTruncatedFieldsThresholdExceeded() )
            truncateStringFieldsKO.truncatedFields += "<strong>This mail is limited to " +
                    businessDataControls.getTruncatedFieldsThreshold() +
                    " messages.</strong>";
        truncateStringFieldsKO.environment = environment;
        String xml = getXMLParamsForEmail(truncateStringFieldsKO);
        emailClient.sendMail(emailTemplateRepo.findByType(TRUNCATE_STRING_FIELDS), xml, list.getScope());
        }
    }

    public void sendEmailIfConstraintViolations(Long versionId, BusinessDataControls businessDataControls) {
        if( businessDataControls.hasConstraintViolationsErrorsOrWarnings() ) {
            Version version = versionRepo.getExactlyOne(versionId);
            ReglissList list = version.getList();
            ConstraintViolations violations = new ConstraintViolations();
            violations.listName = list.getName();
            violations.listRefListName = list.getReference() + " - " + list.getName();
            violations.versionNumber = version.getVersionNumber().toString();
            violations.errorViolations = getErrorViolationMessage(businessDataControls);
            violations.warningViolations = getWarningViolationMessage(businessDataControls);
            violations.environment = environment;

            String xml = getXMLParamsForEmail(violations);
            emailClient.sendMail(emailTemplateRepo.findByType(CONSTRAINT_VIOLATION), xml, list.getScope());
        }
    }

    public void sendEmailExpireDownloadRights(List<String> listSupportAdmins, List<String> usersWithImportExpired, List<String> usersWithExportExpired) {
        String expirationDate = LocalDateTime.now().plusDays(5).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        UsersExpiredDownloadRights usersExpiredDownloadRights = new UsersExpiredDownloadRights();
        usersExpiredDownloadRights.expireImportDate = expirationDate;
        usersExpiredDownloadRights.expireExportDate = expirationDate;
        usersExpiredDownloadRights.setUsersWithImportExpired(usersWithImportExpired);
        usersExpiredDownloadRights.setUsersWithExportExpired(usersWithExportExpired);
        usersExpiredDownloadRights.environment = environment;
        String xml = getXMLParamsForEmail(usersExpiredDownloadRights);
        EmailTemplate byType = emailTemplateRepo.findByType(USERS_EXPIRED_DOWNLOAD_RIGHTS);
        byType.setToAddress(String.join(",", listSupportAdmins));
        emailClient.sendMail(byType, xml);
    }

    public void sendEmailWrongZipHierarchy(String zipFileName, ReglissList list, String structureValue) {
        EmailZipHierarchyNotCompliant emailZipHierarchyNotCompliant = new EmailZipHierarchyNotCompliant();
        emailZipHierarchyNotCompliant.fileName = zipFileName;
        emailZipHierarchyNotCompliant.structureValue = structureValue;
        emailZipHierarchyNotCompliant.listRefListName = list.getReference() + " - " + list.getName();
        emailZipHierarchyNotCompliant.environment = environment;
        String xml = getXMLParamsForEmail(emailZipHierarchyNotCompliant);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_ZIP_HIERARCHY_NOT_COMPLIANT), xml, list.getScope());
    }

    public void sendEmailDJImportLogFileControlKO(long versionId){
        Version version = versionRepo.getExactlyOne(versionId);
        ReglissList list = version.getList();
        LogControlKO logControlKO = new LogControlKO();
        logControlKO.listRefListName = list.getReference() + " - " + list.getName();
        logControlKO.versionNumber = version.getVersionNumber().toString();
        logControlKO.environment = environment;
        String xml = getXMLParamsForEmail(logControlKO);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_LOG_FILE_CONTROL), xml, list.getScope());
    }

    public void sendEmailValidationOk(Version version){
        EmailValidationOk emailValidationOk = new EmailValidationOk();
        emailValidationOk.listName = version.getList().getName();
        emailValidationOk.listRefListName = version.getList().getReference() + " - " + version.getList().getName();
        emailValidationOk.versionNumber = version.getVersionNumber().toString();
        emailValidationOk.environment = environment;
        String xml = getXMLParamsForEmail(emailValidationOk);
        emailClient.sendMail(emailTemplateRepo.findByType(VERSION_VALIDATION_OK), xml, version.getList().getScope());
    }

    public void sendEmailDjImportDeltaPurcentage(Version version){
        EmailDeltaPurcentage emailDeltaPurcentage = new EmailDeltaPurcentage();
        emailDeltaPurcentage.listRefListName = version.getList().getReference() + " - " + version.getList().getName();
        emailDeltaPurcentage.versionNumber = version.getVersionNumber().toString();
        emailDeltaPurcentage.environment = environment;
        String xml = getXMLParamsForEmail(emailDeltaPurcentage);
        emailClient.sendMail(emailTemplateRepo.findByType(DJ_IMPORT_DELTA_PURCENTAGE), xml, version.getList().getScope());

    }

    public void sendEmailForUserSyncWithRefog(File file,int deactivatedUserCount,int modifiedUserCount,String dateTime ){
        UserSyncWithRefog userSyncWithRefog = new UserSyncWithRefog();
        userSyncWithRefog.environment = environment;
        userSyncWithRefog.batchStatus="success";
        userSyncWithRefog.currentTime=dateTime;
        userSyncWithRefog.deactivatedUsersCount=deactivatedUserCount;
        userSyncWithRefog.modifiedUsersCount=modifiedUserCount;
        String xml = getXMLParamsForEmail(userSyncWithRefog);
        String fileName = "REGLISS " + environment +"_Users modification batch report "+ dateTime + ".xlsx";
        EmailTemplate emailTemplate = emailTemplateRepo.findByType(USERS_MODIFIED_DEACTIVATED_NOTIFICATION);
        emailClient.sendMail(emailTemplate, xml, emailTemplate.getAllToAddresses(),emailTemplate.getAllCcAddresses(),file,fileName);
    }

    private String getWarningViolationMessage(BusinessDataControls businessDataControls) {
        String warningViolationMessage = extractViolations(businessDataControls.getConstraintViolationsWarnings());

        if(businessDataControls.wasConstraintViolationsWarningsThresholdExceeded()) {
            warningViolationMessage += "<strong>This mail is limited to " +
                    businessDataControls.getConstraintViolationsWarningsThreshold() +
                    " warning constraint messages.</strong>";
        }
        return warningViolationMessage;
    }

    private String getErrorViolationMessage(BusinessDataControls businessDataControls) {
        String errorViolationMessage = extractViolations(businessDataControls.getConstraintViolationsErrors());

        if(businessDataControls.wasConstraintViolationsErrorsThresholdExceeded()) {
            errorViolationMessage += "<strong>This mail is limited to " +
                    businessDataControls.getConstraintViolationsErrorsThreshold() +
                    " error constraint messages.</strong>";
        }
        return errorViolationMessage;
    }

    private String extractViolations(Set<ConstraintViolation<Validable>> constraintViolations) {
        Set<String> externalReferences = new HashSet<>();
        constraintViolations.forEach(elem -> externalReferences.add(elem.getRootBean().getBusinessId()));

        return externalReferences.stream()
                .map(ref -> writeConstraintViolation(ref, constraintViolations))
                .collect(joining(BR_DELIMITER));
    }

    private EmailPublicationOkUpdateNotification getParamsForUpdateNotification(Version version) {
        ReglissList list = version.getList();
        EmailPublicationOkUpdateNotification emailPublicationOkUpdateNotification = new EmailPublicationOkUpdateNotification();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        emailPublicationOkUpdateNotification.versionNumber = version.getVersionNumber().toString();
        emailPublicationOkUpdateNotification.listRefListName = list.getReference() + " - " + list.getName();
        if (version.getCertificationDate() != null) {
            emailPublicationOkUpdateNotification.certificationDate = version.getCertificationDate().format(formatter);
        }

        ComputedRecordNumbers computedRecordNumbers = version.getComputedRecordNumbers();
        emailPublicationOkUpdateNotification.recordsTotalNumber = String.valueOf(computedRecordNumbers.getTotal());
        emailPublicationOkUpdateNotification.recordsCreated = String.valueOf(computedRecordNumbers.getAdd());
        emailPublicationOkUpdateNotification.recordsUpdated = String.valueOf(computedRecordNumbers.getModForSynthesis());
        emailPublicationOkUpdateNotification.recordsDeleted = String.valueOf(computedRecordNumbers.getSup());
        emailPublicationOkUpdateNotification.programNames = recordRepo.getProgramsForChangedRecords(version.getId(),version.getList().getId())
                .stream().map(Program::getLabelOpt).filter(Optional::isPresent).map(Optional::get)
                .map(t -> "<br>" + t).collect(Collectors.joining());
        emailPublicationOkUpdateNotification.environment = environment;
        return emailPublicationOkUpdateNotification;
    }

    private String getTruncatedFieldsByEntity(TruncatedRecordFieldsValues truncatedRecordFieldsValues){
        String fullName = truncatedRecordFieldsValues.getClassName();
        String recordExternalReference = truncatedRecordFieldsValues.getRecordExternalReference();
        String classType = fullName.substring(fullName.lastIndexOf( '.' )+ 1);
        StringBuilder sb = new StringBuilder();
        truncatedRecordFieldsValues.getTruncateStringList().forEach(item -> sb.append("<strong>Entity type</strong> = ")
                .append(classType)
                .append(" from record with <strong>externalReference</strong> = ")
                .append(recordExternalReference)
                .append(" : ")
                .append("<br><strong>field name</strong> : ")
                .append(item.getFieldName())
                .append("<br><strong>original value</strong> : ")
                .append(item.getOriginalValue())
                .append("<br><strong>truncated value</strong> : ")
                .append(item.getTruncatedValue())
                .append("<br>"));

        return sb.toString();
    }

    private String writeConstraintViolation(String ref, Set<ConstraintViolation<Validable>> constraints){
        StringBuilder sb = new StringBuilder();
        List<String> messages = constraints.stream().filter(f-> filterReference(ref, f.getRootBean().getBusinessId()))
                                        .map(ConstraintViolation::getMessage )
                                        .collect(Collectors.toList());

        sb.append("<br>Record with <strong>externalReference</strong> = ").append(ref).append("<br>");
        messages.forEach(elem -> sb.append("<strong>message</strong> = ").append(elem).append("<br>"));
        return sb.toString();
    }


    private String getXMLParamsForEmail(Serializable emailDto) {
        XmlMapper mapper = new XmlMapper();
        mapper.setDefaultUseWrapper(false);
        try {
            return mapper.writer().withRootName("root").writeValueAsString(emailDto);
        } catch (JsonProcessingException e) {
            throw new ReglissException(e, "Unable to serialize...");
        }
    }


    public boolean filterReference(String ref, String extRef) {
        return Optional.ofNullable(ref).map(elem -> elem.equals(extRef))
                .orElseGet(() -> extRef == null);
    }
}
