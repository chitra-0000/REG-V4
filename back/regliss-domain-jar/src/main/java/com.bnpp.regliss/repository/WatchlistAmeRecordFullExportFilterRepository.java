package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import static com.bnpp.regliss.repository.RecordRepositoryCustom.jpqlRecordInVersionCondition;

public interface WatchlistAmeRecordFullExportFilterRepository extends WatchlistAmeRecordExportFilterRepository, EntityRepository<Record, Long> {

    @Query("SELECT record.id, categories.id, record.type, record.versionLinks.createdInVersionId, record.versionLinks.clonedInVersionId "
            + "FROM Record record "
            + "JOIN Version ver ON record.versionLinks.listId=ver.list.id AND ver.id = :idNewVersion "
            + "LEFT JOIN record.categories categories "
            + "WHERE "+jpqlRecordInVersionCondition)
    List<Object[]> getRecordCategories(@Param("idNewVersion") Long idNewVersion);

    @Query("SELECT record.id, sanctionTypes.id "
            + "FROM Record record "
            + "JOIN Version ver ON record.versionLinks.listId=ver.list.id AND ver.id = :idNewVersion "
            + "INNER JOIN record.sanctionTypes sanctionTypes "
            + "WHERE "+jpqlRecordInVersionCondition)
    List<Object[]> getSanctionTypes(@Param("idNewVersion") Long idNewVersion);

    @Query("SELECT record.id, descriptions3.id "
            + "FROM Record record "
            + "JOIN Version ver ON record.versionLinks.listId=ver.list.id AND ver.id = :idNewVersion "
            + "INNER JOIN record.descriptions3 descriptions3 "
            + "WHERE "+jpqlRecordInVersionCondition)
    List<Object[]> getRecordDescriptions3(@Param("idNewVersion") Long idNewVersion);

    @Query("SELECT record.id, pepOccupationCategory.id "
            + "FROM Record record "
            + "JOIN Version ver ON record.versionLinks.listId=ver.list.id AND ver.id = :idNewVersion "
            + "INNER JOIN record.personFunctions functions "
            + "INNER JOIN functions.pepOccupationCategory pepOccupationCategory "
            + "WHERE "+jpqlRecordInVersionCondition)
    List<Object[]> getRecordPepOccupations(@Param("idNewVersion") Long idNewVersion);

    @Query("SELECT record.id, countriesDetails.country.id "
            + "FROM Record record "
            + "JOIN Version ver ON record.versionLinks.listId=ver.list.id AND ver.id = :idNewVersion "
            + "INNER JOIN record.countriesDetails countriesDetails "
            + "WHERE " +jpqlRecordInVersionCondition
            + "AND countriesDetails.type.businessConcept = :countryType")
    List<Object[]> getRecordCountriesInVersionByBusinessConcept(@Param("idNewVersion") Long idNewVersion, @Param("countryType") ReferentialBusinessConcept countryType);

    @Query("SELECT record.id, biographies.dateValueStr "
            + "FROM Record record "
            + "JOIN Version ver ON record.versionLinks.listId=ver.list.id AND ver.id = :idNewVersion "
            + "INNER JOIN record.biographies biographies "
            + "WHERE "+jpqlRecordInVersionCondition
            + "AND biographies.dateType.businessConcept = :dateType ")
    List<Object[]> getRecordDatesInVersionByBusinessConcept(@Param("idNewVersion") Long idNewVersion, @Param("dateType") ReferentialBusinessConcept dateType);

    @Query("SELECT record.id, genders.id "
            + "FROM Record record "
            + "JOIN Version ver ON record.versionLinks.listId=ver.list.id AND ver.id = :idNewVersion "
            + "INNER JOIN record.genders genders "
            + "WHERE "+jpqlRecordInVersionCondition)
    List<Object[]> getRecordGenders(@Param("idNewVersion") Long idNewVersion);


}

