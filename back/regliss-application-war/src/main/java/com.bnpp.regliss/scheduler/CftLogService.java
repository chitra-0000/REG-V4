package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.entity.CftgtwRequest;
import com.bnpp.regliss.entity.FileDelivery;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.Subscriber;
import com.bnpp.regliss.profile.ReglissPublisherProfile;
import com.bnpp.regliss.repository.CftgtwRequestRepository;
import com.bnpp.regliss.repository.FileDeliveryRepository;
import com.bnpp.regliss.service.FileDeliveryAuditService;
import com.bnpp.regliss.service.FileDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@ReglissPublisherProfile
public class CftLogService {

    @Autowired
    private CftLogRepository cftLogRepo;

    @Autowired
    private FileDeliveryAuditService fileDeliveryAuditService;

    @Autowired
    private FileDeliveryService fileDeliveryService;

    @Autowired
    private FileDeliveryRepository fileDeliveryRepo;

    @Autowired
    private CftgtwRequestRepository cftgtwRequestRepo;

    public List<CftLogEntry> getStatusStrings(String idF, String fileName, String fileExt, List<CftLogEntry> cftLogEntries) {
        try (Stream<CftLogEntry> entries = cftLogEntries.stream()) {
            return entries
                    .filter(entry -> idF.equals(entry.getIdF()) && entry.getFileNameWithExtension().contains(fileName)
                            && entry.getFileNameWithExtension().contains(fileExt))
                    .collect(Collectors.toList());
        }
    }

    public Optional<CftLogEntry> getStatusStrings(List<CftLogEntry> cftLogEntries, String cftLogTimeId) {
        try (Stream<CftLogEntry> entries = cftLogEntries.stream()) {
            return entries.filter(entry -> cftLogTimeId.equals(entry.getTimeId()))
                    .findAny();
        }
    }

    // CFTBNP23+E+MG34117T+M087140R+REGUTID1++/applis/REGLISSTestCFT/FichierTestCFT.txt+K0+406
    // RCO 301+H1710210+FichierTestCFT.t+CANDLEEXIT
    public CftLogEntry parseLine(String line) {
        // One CFT log entry line from IDU.log, should have 12 fields separated by 11 '+' chars!
        try {
            CftLogEntry entry = new CftLogEntry();
            String[] parts = line.split("\\+");
            if (parts.length > 12) {
                log.debug(String.format("Cannot parse line: %s %nMore than %s '+' chars inside line! Check the file name, not to contain '+' char!", line, 11));
            }
            entry.setOriginalLogLine(line);
            entry.setOk("OK".equals(parts[7]));
            entry.setTimeId(parts[9].substring(1));
            entry.setReturnCode(Integer.parseInt(parts[8].substring(0, parts[8].indexOf(' '))));
            entry.setMessage(parts[8].substring(parts[8].indexOf(' ') + 1));
            entry.setPathToTransferredFile(parts[6]);
            entry.setIdF(parts[4]);

            String[] filePath = parts[6].split("/");
            entry.setFileNameWithExtension(filePath[filePath.length - 1].replaceFirst(".zip.pgp", ""));
            return entry;
        } catch (Exception e) {
            log.error(String.format("Could not parse log CFT line: %s%n Exception: %s", line, e.getMessage()));
            return null;
        }
    }

    public List<CftLogEntry> readAndParseCFTLogFile() {
        try (Stream<String> linesStream = cftLogRepo.getLines()) {
            return linesStream.filter(StringUtils::isNotBlank)
                    .map(this::parseLine)
                    .filter(Objects::nonNull).collect(Collectors.toList());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void collectStatusesForFileDeliveries(List<Long> deliveriesIdsInProgress, List<CftLogEntry> cftLogEntries) {
        for (Long deliveryId : deliveriesIdsInProgress) {
                FileDelivery delivery = fileDeliveryRepo.getExactlyOne(deliveryId);
                GeneratedFile generatedFile = delivery.getGeneratedFile();
                Optional<Subscriber> subscriberOpt = generatedFile.getSubscriberOpt();
                String cftFluxName = (generatedFile.isDeliveryReport() && subscriberOpt.isPresent())
                        ? subscriberOpt.get().getCftFluxName()
                        : delivery.getSubscription().getSubscriber().getCftFluxName();

                List<CftLogEntry> cftLogFilteredEntries = getStatusStrings(cftFluxName, generatedFile.getFileName(),
                        "." + generatedFile.getGeneratedFileFormat().getFileExtension().name(), cftLogEntries);

            List<FileDelivery> deliveriesForSubscriberAndFile = fileDeliveryService.getDeliveriesOfSubscriberAndFile(generatedFile, cftFluxName);
                long countInProgress = deliveriesForSubscriberAndFile.stream().filter(f -> f.getStatus().equals(FileDelivery.Status.IN_PROGRESS)).count();
                long diffBetweenDBAndLog = (long) deliveriesForSubscriberAndFile.size() - cftLogFilteredEntries.size();
                if (delivery.getStatus() == FileDelivery.Status.IN_PROGRESS && (delivery.getActualRunTime() == null || Period.between(delivery.getActualRunTime()
                        .toLocalDate(), LocalDate.now()).getDays() > 0)) {
                    fileDeliveryService.obsoleteDelivery(delivery, "IN_PROGRESS delivery older than 24h!");
                }

                if (diffBetweenDBAndLog > 0 && countInProgress == diffBetweenDBAndLog) {
                    log.debug("Waiting for {} file(s) to be processed on CFT", countInProgress);
                    continue;

                } else if (diffBetweenDBAndLog == 0 || countInProgress != diffBetweenDBAndLog) {
                    log.debug("Nr in progress {}. Nr of received and not acknowledged {} for subscriber {} & fileName {}",
                            countInProgress, diffBetweenDBAndLog, cftFluxName, generatedFile.getFileName());
                    setStatus(delivery, cftLogFilteredEntries, deliveriesForSubscriberAndFile);
                }

                if (delivery.getStatus() != FileDelivery.Status.IN_PROGRESS) {
                    fileDeliveryAuditService.auditDelivery(delivery);
                }
            }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void collectStatusesForCftGTWDeliveries(List<Long> cftGWTReqIds) {
        List<CftLogEntry> cftLogEntries = readAndParseCFTLogFile();
        for (Long id : cftGWTReqIds) {
            CftgtwRequest cftgtwRequest = cftgtwRequestRepo.getExactlyOne(id);
            List<String> idsNotFoundInCftLog = new ArrayList<>();
            for (String cftLogTimeId : cftgtwRequest.getRemainingCftLogTimeIds()) {
                Optional<CftLogEntry> statusOpt = getStatusStrings(cftLogEntries, cftLogTimeId);
                if (statusOpt.isPresent()) {
                    CftLogEntry cftLogEntry = statusOpt.get();
                    log.debug(
                            "CFT delivery completed for logTimeId: {} with status: {} and return code: {}. Corresponding IDU.log line is: {}.",
                            cftLogEntry.getTimeId(), cftLogEntry.isOk() ? "OK" : "KO", cftLogEntry.getReturnCode(),
                            cftLogEntry.getOriginalLogLine());
                } else if (Period.between(cftgtwRequest.getRequestTime().toLocalDate(), LocalDate.now()).getDays() == 0) {
                    idsNotFoundInCftLog.add(cftLogTimeId);
                }
            }
            cftgtwRequest.setRemainingCftLogTimeIds(idsNotFoundInCftLog);
            cftgtwRequestRepo.save(cftgtwRequest);
        }
    }

    private void setStatus(FileDelivery delivery, List<CftLogEntry> cftLogEntries,
                           List<FileDelivery> deliveriesForSubscriberAndFile) {
        Optional<CftLogEntry> cftOKLogEntryOpt = cftLogEntries.stream().filter(CftLogEntry::isOk).findAny();

        if (cftOKLogEntryOpt.isPresent()) {
            fileDeliveryService.setOkStatus(delivery, cftOKLogEntryOpt.get().getReturnCode());
        } else if (!deliveriesForSubscriberAndFile.isEmpty() && cftLogEntries.size() > deliveriesForSubscriberAndFile.indexOf(delivery)) {
            CftLogEntry cftLogEntry = cftLogEntries.get(deliveriesForSubscriberAndFile.indexOf(delivery));
            log.debug("Got the status for {} : {}", delivery, cftLogEntry);
            fileDeliveryService.decreaseRetriesLeftOrSetKoStatus(delivery, cftLogEntry.getMessage(), cftLogEntry.getReturnCode());
        }
    }

    public List<Long> getAllIdsInProgress() {
        return fileDeliveryRepo.getAllIdsInProgress();
    }

    public List<Long> getAllIdsByRemainingCftLogTimeIdsIsNotNull() {
        return cftgtwRequestRepo.getAllIdsByRemainingCftLogTimeIdsIsNotNull();
    }

}

