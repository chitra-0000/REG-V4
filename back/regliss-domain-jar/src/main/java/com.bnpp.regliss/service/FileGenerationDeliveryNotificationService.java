package com.bnpp.regliss.service;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.FileDelivery;
import com.bnpp.regliss.entity.FileDelivery.Status;
import com.bnpp.regliss.entity.ServerEventsChannelType;
import com.bnpp.regliss.entity.batch.BatchExportType;
import com.bnpp.regliss.entity.batch.BatchJobExecution;
import com.bnpp.regliss.utils.JacksonUtil;
import com.bnpp.regliss.vo.BatchExportLoggedParameters;
import com.bnpp.regliss.vo.WebsocketNotificationVO;
import com.bnpp.regliss.vo.WebsocketNotificationVO.CompletionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.bnpp.regliss.entity.batch.BatchExportType.ADD_MOD_SUP_FILE;
import static com.bnpp.regliss.entity.batch.BatchExportType.ADD_MOD_SUP_AUDIT_TRAIL_FILE;

@Component
@Slf4j
public class FileGenerationDeliveryNotificationService {

    @Autowired
    private ServerEventsGateway serverEvents;

    private LocalDateTime lastDeliveryTimestamp;
    private Status lastDeliveryStatus;
    private LocalDateTime versionOrOfacLastGenerationTimestamp;
    private LocalDateTime addModSupLastGenerationTimestamp;
    private LocalDateTime addModSupAuditTrailLastGenerationTimestamp;

    public static final String TIME_STAMP ="timestampLastVersionGeneratedFile ";

    public void saveLatestGenerationTimestampInCache(BatchJobExecution batchJobExecution) {
        BatchExportLoggedParameters params = JacksonUtil.getDeserializedValue(batchJobExecution.getJobParams(), BatchExportLoggedParameters.class)
                .orElseThrow(() -> new ReglissException("Could not deserialize batch export execution parameters: " + batchJobExecution.getJobParams()));
        saveLatestGenerationTimestampInCache(params.getExportType(), batchJobExecution.getEndDate());
    }

    public void notifyCompletionDeliveringFiles(FileDelivery fileDelivery) {
        if(isFileDeliveryInCache()){
            if(needToSendDeliveryEvent(fileDelivery)){
                notifyDelivery(ServerEventsChannelType.GENERATION_OR_DELIVERY_FINISHED);
                saveLatestFileDeliveryDataInCache(fileDelivery);
            }
        } else {
            saveLatestFileDeliveryDataInCache(fileDelivery);
        }
    }

    public void notifyCompletionGeneratingFiles(BatchJobExecution batchJobExecution) {
        BatchExportType exportType = JacksonUtil.getDeserializedValue(batchJobExecution.getJobParams(), BatchExportLoggedParameters.class)
                .map(BatchExportLoggedParameters::getExportType)
                .orElseThrow(() -> new ReglissException("Could not deserialize batch export execution parameters: " + batchJobExecution.getJobParams()));
        LocalDateTime latestGenerationTimestampInDb = batchJobExecution.getEndDate();
        if(!isGenerationTimestampInCache(exportType)) {
            saveLatestGenerationTimestampInCache(exportType, latestGenerationTimestampInDb);
        } else if(!latestGenerationTimestampInDb.equals(getCachedGenerationTimestamp(exportType))){
            notifyDelivery(getServerEventsChannelTypeForGeneration(exportType));
            saveLatestGenerationTimestampInCache(exportType, latestGenerationTimestampInDb);
        }
    }

    public void saveLatestFileDeliveryDataInCache(FileDelivery fileDelivery){
        lastDeliveryTimestamp = fileDelivery.getActualRunTime();
        lastDeliveryStatus = fileDelivery.getStatus();
        log.trace("timestampLastDelivery " + lastDeliveryTimestamp);
        log.trace("statusLastDelivery " + lastDeliveryStatus);
    }

    private boolean isFileDeliveryInCache() { return lastDeliveryStatus != null && lastDeliveryTimestamp != null; }

    private boolean needToSendDeliveryEvent(FileDelivery fileDelivery) {
        return (!fileDelivery.getActualRunTime().equals(lastDeliveryTimestamp)
            || !fileDelivery.getStatus().equals(lastDeliveryStatus));
    }

    private void notifyDelivery(ServerEventsChannelType serverEventsChannelType){
        WebsocketNotificationVO notif = new WebsocketNotificationVO(CompletionMessage.DELIVERY);
        serverEvents.broadcastEvent(serverEventsChannelType);
        log.trace("Message sent through websocket: " + notif.getMessage());
    }

    private boolean isGenerationTimestampInCache(BatchExportType exportType){
        return getCachedGenerationTimestamp(exportType) != null;
    }

    private void saveLatestGenerationTimestampInCache(BatchExportType exportType, LocalDateTime newTimestamp){
        if(exportType == ADD_MOD_SUP_FILE){
            addModSupLastGenerationTimestamp = newTimestamp;
            log.trace("timestampLastAddModSupGeneratedFile " + addModSupLastGenerationTimestamp);
        } else if(exportType == ADD_MOD_SUP_AUDIT_TRAIL_FILE){
            addModSupAuditTrailLastGenerationTimestamp = newTimestamp;
            log.trace("timestampLastAddModSupAuditTrailGeneratedFile " + addModSupAuditTrailLastGenerationTimestamp);
        } else {
            versionOrOfacLastGenerationTimestamp = newTimestamp;
            log.trace(TIME_STAMP + versionOrOfacLastGenerationTimestamp);
        }
    }

    private LocalDateTime getCachedGenerationTimestamp(BatchExportType exportType){
        if(exportType == ADD_MOD_SUP_FILE){
            return addModSupLastGenerationTimestamp;
        } else if(exportType == ADD_MOD_SUP_AUDIT_TRAIL_FILE){
            return addModSupAuditTrailLastGenerationTimestamp;
        } else {
            return versionOrOfacLastGenerationTimestamp;
        }
    }

    private ServerEventsChannelType getServerEventsChannelTypeForGeneration(BatchExportType exportType) {
        if(exportType == ADD_MOD_SUP_FILE){
            return ServerEventsChannelType.ADD_MOD_SUP_GENERATION_FINISHED;
        } else if(exportType == ADD_MOD_SUP_AUDIT_TRAIL_FILE){
            return ServerEventsChannelType.ADD_MOD_SUP_AUDIT_TRAIL_GENERATION_FINISHED;
        } else {
            return ServerEventsChannelType.GENERATION_OR_DELIVERY_FINISHED;
        }
    }

}
