package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.FileDelivery;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.Subscriber;
import com.bnpp.regliss.entity.Subscription;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileDeliveryRepository extends EntityRepository<FileDelivery, Long>, FileDeliveryRepositoryCustom { //NOSONAR

    @Query("SELECT fd0 FROM FileDelivery fd0 "
            + "WHERE fd0.id = (SELECT min(fd1.id) "
                            + "FROM FileDelivery fd1 "
                            + "WHERE fd1.status = 'SCHEDULED' "
                            + "AND fd1.scheduledRunTime=(SELECT min(fd2.scheduledRunTime) "
                                                        + "FROM FileDelivery fd2 "
                                                        + "WHERE fd2.status = 'SCHEDULED' "
                                                        + "AND fd2.scheduledRunTime < ?1)"
                            + ")")
    Optional<FileDelivery> getOldestNotProcessed(LocalDateTime currentTime);

    @Query("SELECT fd.id FROM FileDelivery fd WHERE fd.status = 'IN_PROGRESS'")
    List<Long> getAllIdsInProgress();

    List<FileDelivery> getByGeneratedFileAndSubscription(GeneratedFile generatedFile, Subscription subscription);

    @Query("SELECT fd FROM FileDelivery fd WHERE fd.generatedFile.version.id = ?1 "
            + "AND fd.generatedFile.generatedFileFormat.fileFormat != 'DELIVERY_REPORT'")
    List<FileDelivery> getVersionDeliveries(Long versionId);

    @Query("SELECT fd FROM FileDelivery fd WHERE fd.generatedFile.version.id = ?1 "
            + "AND fd.generatedFile.generatedFileFormat.fileFormat != 'DELIVERY_REPORT' "
            + "AND (fd.status = 'SCHEDULED' OR fd.status = 'IN_PROGRESS')")
    List<FileDelivery> getUncompletedVersionDeliveries(Long versionId);

    @Query("SELECT fd FROM FileDelivery fd WHERE fd.generatedFile.version IS NULL "
            + "AND (fd.status = 'SCHEDULED' OR fd.status = 'IN_PROGRESS')")
    List<FileDelivery> getUncompletedOfacDeliveries();

    @Query("SELECT fd FROM FileDelivery fd WHERE fd.generatedFile.subscriber = ?1 "
            + "AND fd.generatedFile.generatedFileFormat.fileFormat = 'DELIVERY_REPORT' "
            + "AND fd.status = 'SCHEDULED' AND fd.status = 'IN_PROGRESS'")
    List<FileDelivery> getUncompletedPVDeliveriesForSubscriber(Subscriber subscriber);


    @Query("SELECT fdWebSocket FROM FileDelivery fdWebSocket "
            + " LEFT JOIN FETCH fdWebSocket.deliveryUser "
            + " LEFT JOIN FETCH fdWebSocket.generatedFile "
            + " LEFT JOIN FETCH fdWebSocket.subscription s "
            + " LEFT JOIN FETCH s.subscriber "
            + "WHERE fdWebSocket.id = (SELECT max(fd2.id) "
                            + "FROM FileDelivery fd2 "
                            + "WHERE fd2.status IN ('IN_PROGRESS', 'OK', 'KO') "
                            + "AND fd2.actualRunTime = (SELECT max(fd3.actualRunTime) "
                                + "FROM FileDelivery fd3 "
                                + "WHERE fd3.status IN ('IN_PROGRESS', 'OK', 'KO') AND fd3.actualRunTime IS NOT NULL))")
    Optional<FileDelivery> getLatestStartedOrFinished();

    @Query("SELECT fd FROM FileDelivery fd WHERE trunc(fd.scheduledRunTime) = trunc(sysdate) and fd.actualRunTime is not null and fd.subscription.subscriber.cftFluxName = ?1 "
            + "AND fd.generatedFile.fileName = ?2 and fd.generatedFile.generatedFileFormat.fileExtension = ?3 order by id")
    List<FileDelivery> attemptedDeliveriesForSubscriberAndFile(String cftFluxName, String fileName, FileExportExtension ext);

    @Query("SELECT fd FROM FileDelivery fd WHERE trunc(fd.scheduledRunTime) = trunc(sysdate) and fd.actualRunTime is not null and fd.generatedFile.subscriber.cftFluxName = ?1 "
            + "AND fd.generatedFile.fileName = ?2 and fd.generatedFile.generatedFileFormat.fileExtension = ?3 order by id")
    List<FileDelivery> attemptedReportDeliveriesForSubscriberAndFile(String cftFluxName, String fileName, FileExportExtension ext);

    String SCHEDULED_DELIVERIES_OLD_VERSIONS_QUERY = "SELECT\n" +
        "    fd.*\n" +
        "FROM\n" +
        "    file_delivery    fd,\n" +
        "    generated_file   gf,\n" +
        "    version          v,\n" +
        "    regliss_list     l\n" +
        "WHERE\n" +
        "    fd.status = 'SCHEDULED'\n" +
        "    AND fd.generated_file_id = gf.id\n" +
        "    AND gf.version_id = v.id\n" +
        "    AND v.list_id = l.id\n" +
        "    AND v.version_number < (\n" +
        "        SELECT\n" +
        "            MAX(version_number)\n" +
        "        FROM\n" +
        "            version          v2,\n" +
        "            generated_file   gf2,\n" +
        "            file_delivery    fd2\n" +
        "        WHERE\n" +
        "            v2.list_id = l.id\n" +
        "            AND gf2.version_id = v2.id\n" +
        "            AND gf2.id = fd2.generated_file_id\n" +
        "            AND fd2.status = 'SCHEDULED'\n" +
        "    )" +
        "--GLOBALPLUSOFAC\n" +
        "UNION ALL\n" +
        "SELECT\n" +
        "    fd.*\n" +
        "FROM\n" +
        "    file_delivery   fd,\n" +
        "    subscription    sbs\n" +
        "WHERE\n" +
        "    fd.status = 'SCHEDULED'\n" +
        "    AND fd.subscription_id = sbs.id\n" +
        "    AND sbs.list_id IS NULL\n" +
        "    AND fd.id < (\n" +
        "        SELECT\n" +
        "            MAX(id)\n" +
        "        FROM\n" +
        "            file_delivery fd2\n" +
        "        WHERE\n" +
        "            fd2.subscription_id = sbs.id\n" +
        "            AND fd2.status = 'SCHEDULED'\n" +
        "    )";
}

