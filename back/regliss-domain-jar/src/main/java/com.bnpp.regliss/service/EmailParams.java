package com.bnpp.regliss.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// disable Sonar Rule because this class contains simple email templates DTOs with no logic thus fields were made public to simplify the code
/squid:ClassVariableVisibilityCheck/
public class EmailParams {

    private EmailParams(){}

    public static class EmailPublicationOk extends EmailBaseParams implements Serializable {
        public String listRefListName;
        public String versionNumber;
    }

    public static class EmailPublicationKo extends EmailBaseParams implements Serializable {
        public String listRefListName;
        public String versionNumber;
        public String fileNames;
    }

    public static class EmailAutomaticVersionDeliveryOk extends EmailBaseParams implements Serializable {
        public String listRefListName;
        public String versionNumber;
    }

    public static class EmailDeliveryReportKo extends EmailBaseParams implements Serializable {
        public String subscriberName;
        public String listRefListName;
        public String versionNumber;
    }

    public static class EmailDjImportConfirmation extends EmailBaseParams implements Serializable {
        public String listRefListName;
        public String versionNumber;
        public String versionHyperlink;
    }

    public static class EmailDjImportConfirmationRequired extends EmailBaseParams implements Serializable {
        public String listName;
        public String listRefListName;
        public String versionNumber;
        public String versionHyperlink;
    }

    public static class EmailDjImportCftError extends EmailBaseParams implements Serializable {
        public String listRefListName;
    }

    public static class UsersExpiredDownloadRights extends EmailBaseParams implements Serializable {
        public String expireImportDate;
        public String expireExportDate;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "usersWithImportExpired")
        private List<String> usersWithImportExpired = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "usersWithExportExpired")
        private List<String> usersWithExportExpired = new ArrayList<>();

        public List<String> getUsersWithImportExpired() { return usersWithImportExpired; }

        public void setUsersWithImportExpired(List<String> usersWithImportExpired) {
            this.usersWithImportExpired = usersWithImportExpired;
        }

        public List<String> getUsersWithExportExpired() { return usersWithExportExpired; }

        public void setUsersWithExportExpired(List<String> usersWithExportExpired) {
            this.usersWithExportExpired = usersWithExportExpired;
        }
    }

    public static class EmailDjImportZipError extends EmailBaseParams implements Serializable {
        public String listRefListName;
        public String errorMessage;
    }

    public static class EmailDjImportChecksumError extends EmailBaseParams implements Serializable {
        public String listRefListName;
    }

    public static class EmailDjImportOldDateError extends EmailBaseParams implements Serializable {
        public String listRefListName;
    }

    public static class EmailCaImportWrongOrderingMarker extends EmailBaseParams implements Serializable {
        public String listRefListName;
    }

    public static class EmailDjImportFileStructureError extends EmailBaseParams implements Serializable {
        public String listRefListName;
    }

    public static class EmailDjImportLogLivraisonCountError extends EmailBaseParams implements Serializable {
        public String listRefListName;
        public String deliveryLog;
        public String errorMessage;
    }

    public static class EmailDjImportGeneralError extends EmailBaseParams implements Serializable {
        public String listRefListName;
        public String errorMessage;
    }

    public static class EmailDjImportSuccess extends EmailBaseParams implements Serializable {
        public String listRefListName;
        public String versionNumber;
    }

    public static class EmailDjImportFileOnHoldError extends EmailBaseParams implements Serializable {
        public String listName;
        public String listRefListName;
        public String versionNumber;
        public String versionHyperlink;
    }

    // TODO: [GAP - lines 125-129 not captured in screenshots]

    public static class EmailNomenclatureError extends EmailBaseParams implements Serializable {
        public String fileNames;
    }


    public static class EmailDjImportFilesWithInvalidPrefixError extends EmailBaseParams implements Serializable {
        public String fileNames;

    }


    public static class EmailDjImportLowDiskSpace extends EmailBaseParams implements Serializable {
    }

    public static class EmailPublicationOkUpdateNotification extends EmailBaseParams implements Serializable {
        public String listRefListName;
        public String versionNumber;
        public String certificationDate;
        public String programNames;
        public String recordsTotalNumber;
        public String recordsCreated;
        public String recordsUpdated;
        public String recordsDeleted;
    }


    public static class EmailImportError extends EmailBaseParams implements Serializable {
        public String listName;
        public String listRefListName;
        public String versionNumber;
        public String errorMessage;
    }

    public static class EmailExportError extends EmailBaseParams implements Serializable {
        public String listName;
        public String listRefListName;
        public String versionNumber;
        public String errorMessage;
    }

    public static class EmailDjImportFutureDateError extends EmailBaseParams implements Serializable {
        public String listName;
        // TODO: [GAP - rest of EmailDjImportFutureDateError class body not captured in screenshots]
    }

    // TODO: [GAP - lines approx. 169-171 not captured in screenshots, between EmailDjImportFutureDateError and EmailHeartbeatNodeDown]

    public static class EmailHeartbeatNodeDown extends EmailBaseParams implements Serializable {
        public String nodeId;
        public String hostName;
        public String lastHeartbeatDate;
    }

    public static class EmailNewActivePublisher extends EmailBaseParams implements Serializable {
        public String oldNodeId;
        public String oldHostName;
        public String lastHeartbeatDate;
        public String newNodeId;
        public String newHostName;
    }

    public static class EmailFileDeliveryKo extends EmailBaseParams implements Serializable {
        public String listRefListName;
        public String versionNumber;
        public String generatedFileName;
        public String errorMessage;
        public String scheduledRuntime;
        public Integer returnCode;
    }


    public static class EmailBatchJobStalled extends EmailBaseParams implements Serializable {
        public String jobType;
        public String nodeId;
        public String hostName;
        public Float percentage;
        public String lastUpdateDate;
        public String versionNumber;

    }

    public static class EmailUnLockListKO extends EmailBaseParams implements Serializable {
        public Long listId;
        public String listName;
        public String listRefListName;
        public String versionNumber;
        public String userName;
    }

    // TODO: [GAP - lines approx. 209-210 not captured in screenshots]

    public static class TruncateStringFieldsKO extends EmailBaseParams implements Serializable {
        public String versionNumber;
        public String listName;
        public String listRefListName;
        public String truncatedFields;
    }

    public static class ConstraintViolations extends EmailBaseParams implements Serializable {
        public String versionNumber;
        public String listName;
        public String listRefListName;
        public String errorViolations;
        public String warningViolations;
    }

    public static class EmailZipHierarchyNotCompliant extends EmailBaseParams implements Serializable {
        public String fileName;
        public String structureValue;
        public String listRefListName;
    }

    public static class LogControlKO extends EmailBaseParams implements Serializable {
        public String versionNumber;
        public String listRefListName;
    }

    public static class EmailValidationOk extends EmailBaseParams implements Serializable {
        public String listName;
        public String listRefListName;
        public String versionNumber;
    }

    public static class EmailDeltaPurcentage extends EmailBaseParams implements Serializable {
        public String listRefListName;
        public String versionNumber;
    }

    public static class UserSyncWithRefog extends EmailBaseParams implements Serializable {
        public String batchStatus;
        public int modifiedUsersCount;
        public int deactivatedUsersCount;
        public String currentTime;
    }
}
