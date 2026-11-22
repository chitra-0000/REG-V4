package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/*This class is mostly native sql. In this case trying to factor into constants all the repeated literals
 * that appear in queries will just complicate them further.
 * For this reason we mark sonar rule: <String literals should not be duplicated -> squid:S1192> as false positive in this
 * class. Also extensive testing was performed on this queries through com.bnpp.regliss.repository.AddModSupSearchExportBehaviorLocalIT
 * behavior driven tests.
 * */
/squid:S1192/
public interface AddModSupRepository extends EntityRepository<Record, Long>, AddModSupRepositoryCustom { //NOSONAR
    String MAIN_QUERY_WITH_CLAUSE = " WITH version_add_sup_audits AS \n" +
        "                    (SELECT /*+ materialize */ \n" +
        "                           A.id\n" +
        "                            ,A.audited_action\n" +
        "                            ,A.audited_field\n" +
        "                            ,A.object_id\n" +
        "                            ,A.child_id\n" +
        "                            ,A.old_value\n" +
        "                     FROM \n" +
        "                           audits A \n" +
        "                     WHERE \n" +
        "                           A.part_version_id = :versionId\n" +
        "                           AND A.audited_action IN ('RECORD_CREATION','RECORD_DELETION')\n" +
        "                           AND A.audited_field IS NULL ), \n" +
        " version_audits AS\n" +
        "    (SELECT /*+ materialize */\n" +
        "         A.id\n" +
        "          ,A.audited_action\n" +
        "          ,A.audited_field\n" +
        "          ,A.object_id\n" +
        "          ,A.child_id\n" +
        "          ,A.old_value\n" +
        "          ,A.new_value\n" +
        "     FROM\n" +
        "          audits A\n" +
        "     WHERE\n" +
        "         A.part_version_id = :versionId\n" +
        " ), " +
        "     created_deleted_records_ids AS\n" +
        "    (SELECT /*+ materialize */\n" +
        "         id record_id\n" +
        "         ,'ADD' action\n" +
        "     FROM\n" +
        "          record\n" +
        "     WHERE created_in_version_id = :versionId and CLONED_IN_VERSION_ID is NULL\n" +
        "     UNION ALL\n" +
        "     SELECT\n" +
        "            id record_id\n" +
        "         ,'SUP' action\n" +
        "     FROM\n" +
        "          record\n" +
        "     WHERE deleted_in_version = :versionId ) ";


    String PRIMARY_NAME_ADD_SUP_QUERY = " SELECT\n" +
        "            S.object,\n" +
        "            S.action,\n" +
        "            S.record_id,\n" +
        "            S.alias_reference,\n" +
        "            new_value,\n" +
        "            old_value,\n" +
        "            S.child_id,\n" +
        "            S.audited_action audited_action,\n" +
        "            NULL child_type,\n" +
        "            1 field_type_sort_priority\n" +
        "     FROM\n" +
        "          (SELECT 'PRIMARY_NAME' object,\n" +
        "                  CASE\n" +
        "                    WHEN audited_action = 'RECORD_CREATION'     THEN 'ADD'\n" +
        "                    WHEN audited_action = 'RECORD_DELETION'     THEN 'SUP'\n" +
        "                      END action,\n" +
        "                  null alias_reference,\n" +
        "                  CASE WHEN audited_action = 'RECORD_CREATION' THEN NULL\n" +
        "                       WHEN audited_action = 'RECORD_DELETION' THEN     nvl(R.first_name, '')\n" +
        "                                                                          || '@@@'\n" +
        "                                                                          || nvl(R.middle_name, '')\n" +
        "                                                                          || '@@@'\n" +
        "                                                                          || nvl(R.last_name, '')\n" +
        "                                                                          || '@@@'\n" +
        "                                                                          || nvl(R.name, '')\n" +
        "                                                                          || '@@@'\n" +
        "                                                                          || nvl(R.suffix, '')\n" +
        "                      END  old_value,\n" +
        "                  CASE\n" +
        "                    WHEN audited_action = 'RECORD_DELETION' THEN NULL\n" +
        "                    ELSE    nvl(R.first_name, '')\n" +
        "                             || '@@@'\n" +
        "                             || nvl(R.middle_name, '')\n" +
        "                             || '@@@'\n" +
        "                             || nvl(R.last_name, '')\n" +
        "                             || '@@@'\n" +
        "                             || nvl(R.name, '')\n" +
        "                             || '@@@'\n" +
        "                             || nvl(R.suffix, '')\n" +
        "                      END  new_value,\n" +
        "                  S.audited_action,\n" +
        "                  R.id record_id,\n" +
        "                  NULL child_id\n" +
        "           FROM\n" +
        "                (SELECT\n" +
        "                       S.audited_action\n" +
        "                     ,S.audited_field\n" +
        "                     ,s.old_value\n" +
        "                     --,ROW_NUMBER() OVER (PARTITION BY S.audited_action,S.audited_field  ORDER BY S.id) old_value\n" +
        "                     ,S.object_id\n" +
        "                FROM\n" +
        "                     version_add_sup_audits S\n" +
        "                ) S\n" +
        "                 JOIN record R ON S.object_id=R.id) S\n ";


    String PRIMARY_NAME_MOD_QUERY = " SELECT\n" +
        "            S.object,\n" +
        "            'MOD' action,\n" +
        "            S.record_id,\n" +
        "            S.alias_reference,\n" +
        "            new_value,\n" +
        "            old_value,\n" +
        "            S.child_id child_id,\n" +
        "            S.audited_action audited_action,\n" +
        "            NULL child_type,\n" +
        "            1 field_type_sort_priority\n" +
        "\n" +
        "     FROM\n" +
        "          (SELECT 'PRIMARY_NAME' object,\n" +
        "                  null alias_reference,\n" +
        "                  COALESCE(s.person_first_name,R.first_name, '')\n" +
        "                    || '@@@'\n" +
        "                    || COALESCE(s.person_middle_name,R.middle_name, '')\n" +
        "                    || '@@@'\n" +
        "                    || COALESCE(s.person_last_name,R.last_name, '')\n" +
        "                    || '@@@'\n" +
        "                    || COALESCE(s.record_name,R.name, '')\n" +
        "                                 old_value,\n" +
        "                  nvl(R.first_name, '')\n" +
        "                    || '@@@'\n" +
        "                    || nvl(R.middle_name, '')\n" +
        "                    || '@@@'\n" +
        "                    || nvl(R.last_name, '')\n" +
        "                    || '@@@'\n" +
        "                    || nvl(R.name, '')\n" +
        "                                 new_value,\n" +
        "                  S.audited_action,\n" +
        "                  R.id record_id,\n" +
        "                  NULL child_id\n" +
        "           FROM\n" +
        "                (SELECT\n" +
        "                    audited_action\n" +
        "                   ,object_id\n" +
        "                    ,MAX(CASE WHEN audited_field='PERSON_FIRST_NAME' THEN old_value ELSE NULL END) person_first_name\n" +
        "                    ,MAX(CASE WHEN audited_field='PERSON_LAST_NAME' THEN old_value ELSE NULL END) person_last_name\n" +
        "                    ,MAX(CASE WHEN audited_field='PERSON_MIDDLE_NAME' THEN old_value ELSE NULL END) person_middle_name\n" +
        "                    ,MAX(CASE WHEN audited_field='RECORD_NAME' THEN old_value ELSE NULL END) record_name\n" +
        "                FROM\n" +
        "                      (SELECT\n" +
        "                           *\n" +
        "                       FROM\n" +
        "                            (SELECT\n" +
        "                                    S.audited_action\n" +
        "                                 ,S.audited_field\n" +
        "                                 ,old_value\n" +
        "                                 ,ROW_NUMBER() OVER (PARTITION BY audited_action,audited_field,object_id ORDER BY S.id) rn\n" +
        "                                 ,S.object_id\n" +
        "                             FROM\n" +
        "                                  version_audits S\n" +
        "                             WHERE\n" +
        "                                 S.audited_action ='RECORD_MODIFICATION'\n" +
        "                               AND S.audited_field IN ( 'PERSON_FIRST_NAME','PERSON_LAST_NAME','PERSON_MIDDLE_NAME','RECORD_NAME')\n" +
        "                            ) S\n" +
        "                       WHERE S.RN=1)" +
        "                GROUP BY\n" +
        "                         audited_action\n" +
        "                    ,object_id) S\n" +
        "                 JOIN record R ON S.object_id=R.id) S\n ";


    String ALIAS_ADD_SUP_QUERY = " SELECT\n" +
        "            S.object,\n" +
        "            S.action,\n" +
        "            S.record_id,\n" +
        "            S.alias_reference,\n" +
        "            new_value,\n" +
        "            old_value,\n" +
        "            child_id,\n" +
        "            CASE WHEN action = 'ADD' THEN 'RECORD_CREATION' " +
        "            ELSE 'RECORD_DELETION' END audited_action,\n" +
        "            'ALIAS' child_type,\n" +
        "             2 field_type_sort_priority\n" +
        "     FROM\n" +
        "          (SELECT case when aq.business_concept = 'ALIAS_QUALITY_STRONG' then 'ALIAS_STRONG'\n" +
        "                       when aq.business_concept = 'ALIAS_QUALITY_WEAK' then  'ALIAS_WEAK'\n"   +
        "                       when aq.business_concept = 'ALIAS_QUALITY_WEAK_REC' then  'ALIAS_WEAK_REC'\n"   +
        "                       else 'ALIAS_OTHER' end object,\n" +
        "                  record_regliss_reference || regliss_reference_suffix alias_reference,\n" +
        "                  CASE WHEN action = 'ADD' THEN NULL\n" +
        "                        ELSE                       nvl(R.first_name, '')\n" +
        "                                                     || '@@@'\n" +
        "                                                     || nvl(R.middle_name, '')\n" +
        "                                                     || '@@@'\n" +
        "                                                     || nvl(R.last_name, '')\n" +
        "                                                     || '@@@'\n" +
        "                                                     || nvl(R.full_name, '')\n" +
        "                                                     || '@@@'\n" +
        "                                                     || nvl(R.suffix, '')\n" +

        "                      END  old_value,\n" +
        "                  CASE\n" +
        "                    WHEN action = 'SUP' THEN NULL\n" +
        "                      ELSE    nvl(R.first_name, '')\n" +
        "                                || '@@@'\n" +
        "                                || nvl(R.middle_name, '')\n" +
        "                                || '@@@'\n" +
        "                                || nvl(R.last_name, '')\n" +
        "                                || '@@@'\n" +
        "                                || nvl(R.full_name, '')\n" +
        "                                || '@@@'\n" +
        "                                || nvl(R.suffix, '')\n" +
        "                      END  new_value,\n" +
        "                  S.action,\n" +
        "                  S.object_id record_id,\n" +
        "                  S.child_id child_id\n" +
        "           FROM\n" +
        "                ( SELECT\n" +
        "                         action action\n" +
        "                      ,NULL audited_field\n" +
        "                      ,NULL old_value\n" +
        "                      ,RA.record_id object_id\n" +
        "                      ,RA.alias_id child_id\n" +
        "                  FROM\n" +
        "                       created_deleted_records_ids R\n" +
        "                         INNER JOIN record_alias RA ON ra.record_id = r.record_id\n" +
        "                ) S\n" +
        "                  JOIN alias R ON S.child_id=R.id\n" +
        "                  LEFT JOIN REF_alias_quality aq on aq.id = R.ALIAS_QUALITY_ID) S\n ";

    String ALIAS_MOD_QUERY = "      SELECT\n" +
        "\t\t             S.object,\n" +
        "\t\t             'MOD' action,\n" +
        "\t\t             S.record_id,\n" +
        "\t\t             S.alias_reference,\n" +
        "\t\t             new_value,\n" +
        "\t\t             old_value,\n" +
            "             s.child_id,\n" +
            "             S.audited_action,\n" +
            "             'ALIAS' child_type,\n" +
            "                  2 field_type_sort_priority\n" +
        "\t\t\n" +
        "\t\t     FROM\n" +
        "\t\t          (SELECT CASE WHEN aq.business_concept = 'ALIAS_QUALITY_STRONG' then 'ALIAS_STRONG'\n" +
        "\t\t                   WHEN aq.business_concept = 'ALIAS_QUALITY_WEAK' then  'ALIAS_WEAK'\n" +
        "\t\t                   WHEN aq.business_concept = 'ALIAS_QUALITY_WEAK_REC' then  'ALIAS_WEAK_REC'\n" +
        "\t\t                   ELSE 'ALIAS_OTHER' end object,\n" +
        "\t\t                  record_regliss_reference || regliss_reference_suffix alias_reference,\n" +
        "\t\t                  CASE WHEN audited_action = 'RECORD_ALIAS_ADDITION' THEN NULL\n" +
        "\t\t                       WHEN audited_action = 'RECORD_ALIAS_DELETION' THEN     nvl(R.first_name, '')\n" +
        "\t\t                                                                                || '@@@'\n" +
        "\t\t                                                                                || nvl(R.middle_name, '')\n" +
        "\t\t                                                                                || '@@@'\n" +
        "\t\t                                                                                || nvl(R.last_name, '')\n" +
        "\t\t                                                                                || '@@@'\n" +
        "\t\t                                                                                || nvl(R.full_name, '')\n" +
        "\t\t                                                                                || '@@@'\n" +
        "\t\t                                                                                || nvl(R.suffix, '')\n" +
        "\t\t                       ELSE COALESCE(s.person_first_name,R.first_name, '')\n" +
        "\t\t                              || '@@@'\n" +
        "\t\t                              || COALESCE(s.person_middle_name,R.middle_name, '')\n" +
        "\t\t                              || '@@@'\n" +
        "\t\t                              || COALESCE(s.person_last_name,R.last_name, '')\n" +
        "\t\t                              || '@@@'\n" +
        "\t\t                              || COALESCE(s.full_name,R.full_name, '')\n" +
        "\t\t                              || '@@@'\n" +
        "\t\t                              || COALESCE(s.suffix,R.suffix, '')\n" +
        "\t\t\n" +
        "\t\t                      END  old_value,\n" +
        "\t\t                  CASE\n" +
        "\t\t                    WHEN audited_action = 'RECORD_ALIAS_DELETION' THEN NULL\n" +
        "\t\t                    ELSE    nvl(R.first_name, '')\n" +
        "\t\t                              || '@@@'\n" +
        "\t\t                              || nvl(R.middle_name, '')\n" +
        "\t\t                              || '@@@'\n" +
        "\t\t                              || nvl(R.last_name, '')\n" +
        "\t\t                              || '@@@'\n" +
        "\t\t                              || nvl(R.full_name, '')\n" +
        "\t\t                              || '@@@'\n" +
        "\t\t                              || nvl(R.suffix, '')\n" +
        "\t\t                      END  new_value,\n" +
        "\t\t                  S.audited_action,\n" +
        "\t\t                  S.object_id record_id,\n" +
        "\t\t                  S.child_id child_id,\n" +
            "                       count(*) over (partition by object_id, child_id, R.first_name||'@@@'||R.middle_name||'@@@'||R.last_name|| '@@@'||R.full_name|| '@@@'||R.suffix ," +
            " audited_field) cnt\n" +
        "\t\t          FROM\n" +
        "\t\t                (SELECT\n" +
        "\t\t                         audited_action\n" +
        "\t\t                      ,object_id\n" +
        "\t\t                      ,child_id\n" +
        "\t\t                      ,MAX(CASE WHEN audited_action='RECORD_ALIAS_MODIFICATION' THEN 1 ELSE NULL END) audited_field\n" +
        "\t\t                      ,MAX(CASE WHEN audited_field='RECORD_ALIAS_FIRST_NAME' THEN old_value ELSE NULL END) person_first_name\n" +
        "\t\t                      ,MAX(CASE WHEN audited_field='RECORD_ALIAS_LAST_NAME' THEN old_value ELSE NULL END) person_last_name\n" +
        "\t\t                      ,MAX(CASE WHEN audited_field='RECORD_ALIAS_MIDDLE_NAME' THEN old_value ELSE NULL END) person_middle_name\n" +
        "\t\t                      ,MAX(CASE WHEN audited_field='RECORD_ALIAS_FULL_NAME' THEN old_value ELSE NULL END) full_name\n" +
        "\t\t                      ,MAX(CASE WHEN audited_field='RECORD_ALIAS_SUFFIX' THEN old_value ELSE NULL END) suffix\n" +
        "\t\t                 FROM\n" +
        "\t\t                       (SELECT\n" +
        "\t\t                              S.audited_action\n" +
        "\t\t                            ,S.audited_field\n" +
        "\t\t                            ,old_value\n" +
        "\t\t                            ,S.object_id\n" +
        "\t\t                            ,S.child_id\n" +
        "\t\t                        FROM\n" +
        "\t\t                             (SELECT\n" +
        "\t\t                                     S.audited_action\n" +
        "\t\t                                  ,S.audited_field\n" +
        "\t\t                                  ,old_value\n" +
        "\t\t                                  ,ROW_NUMBER() OVER (PARTITION BY audited_action,audited_field,object_id,child_id ORDER BY S.id) rn\n" +
        "\t\t                                  ,S.object_id\n" +
        "\t\t                                  ,S.child_id\n" +
        "\t\t                              FROM\n" +
        "\t\t                                     version_audits S\n" +
        "\t\t                                WHERE\n" +
        "\t\t                                    S.audited_action IN ('RECORD_ALIAS_MODIFICATION','RECORD_ALIAS_ADDITION','RECORD_ALIAS_DELETION')\n" +
        "\t\t                                  AND (S.audited_field IN ( 'RECORD_ALIAS_FIRST_NAME','RECORD_ALIAS_LAST_NAME','RECORD_ALIAS_MIDDLE_NAME','RECORD_ALIAS_FULL_NAME'," +
            "'RECORD_ALIAS_SUFFIX')\n" +
        "\t\t                                         OR S.audited_field IS NULL)\n" +
        "\t\t                               ) S WHERE rn=1 ) S\n" +
        "\t\t                  GROUP BY\n" +
        "\t\t                            audited_action\n" +
        "\t\t                       ,object_id\n" +
        "\t\t                       ,child_id) S\n" +
        "\t\t                   JOIN alias R ON S.child_id=R.id \n" +
        "\t\t                   LEFT JOIN REF_alias_quality aq on aq.id = R.ALIAS_QUALITY_ID ) S \n" +
        "WHERE S.cnt<2";

    String PASSPORT_ADD_SUP_QUERY = " SELECT\n" +
        "             S.object,\n" +
        "             S.action,\n" +
        "             S.record_id,\n" +
        "             S.alias_reference,\n" +
        "             new_value,\n" +
        "             old_value,\n" +
        "             child_id,\n" +
        "             CASE WHEN action = 'ADD' THEN 'RECORD_CREATION' " +
        "                  ELSE 'RECORD_DELETION' END audited_action,\n" +
        "             'IDENTITY' child_type,\n" +
             "            4 field_type_sort_priority\n" +
        "\n" +
        "     FROM\n" +
        "          (SELECT 'PASSPORT' object,\n" +
        "                  audited_action action,\n" +
        "                  NULL alias_reference,\n" +
        "                  CASE WHEN audited_action = 'ADD' THEN  NULL\n" +
        "                       ELSE R.identity_document_number\n" +
        "                      END  old_value,\n" +
        "                  CASE WHEN audited_action  = 'SUP' THEN NULL\n" +
        "                       ELSE\n" +
        "                      R.identity_document_number\n" +
        "                      END  new_value,\n" +
        "                  S.audited_action,\n" +
        "                  S.record_id, \n" +
        "                  S.child_id child_id\n" +
        "           FROM\n" +
        "                (SELECT\n" +
        "                         R.action audited_action\n" +
        "                      ,NULL old_value\n" +
        "                      ,RA.record_id\n" +
        "                      ,RA.identity_document_id child_id\n" +
        "                 FROM\n" +
        "                      created_deleted_records_ids R\n" +
        "                        INNER JOIN record_identity_document RA ON RA.record_id = R.record_id) S\n" +
        "                  JOIN identity_document R ON S.child_id=R.id\n" +
        "                  JOIN ref_identity_document_type DT ON DT.id = R.identity_document_type_id\n" +
        "           WHERE\n" +
        "               DT.business_concept = 'IDT_PASSPORT') S\n ";

    String PASSPORT_MOD_QUERY = " SELECT\n" +
        "             S.object,\n" +
        "             'MOD' action,\n" +
        "             S.record_id,\n" +
        "             S.alias_reference,\n" +
        "             new_value,\n" +
        "             old_value,\n" +
        "             s.child_id,\n" +
        "             S.audited_action,\n" +
        "             'IDENTITY' child_type,\n" +
             "              4 field_type_sort_priority\n" +
        "\n" +
        "     FROM\n" +
        "              (SELECT 'PASSPORT' object,\n" +
        "                  NULL alias_reference,\n" +
        "                  CASE WHEN audited_action = 'RECORD_IDENTITY_DOCUMENT_ADDITION' THEN  NULL\n" +
        "                       WHEN audited_action = 'RECORD_IDENTITY_DOCUMENT_MODIFICATION' THEN   COALESCE(s.old_value,R.identity_document_number)\n" +
        "                       ELSE R.identity_document_number\n" +
        "                      END  old_value,\n" +
        "                  CASE WHEN audited_action  = 'RECORD_IDENTITY_DOCUMENT_DELETION' THEN NULL\n" +
        "                       ELSE\n" +
        "                      R.identity_document_number\n" +
        "                      END  new_value,\n" +
        "                  S.audited_action,\n" +
        "                  S.object_id record_id,\n" +
        "                  S.child_id child_id,\n" +
        "                   count(*) over (partition by object_id, child_id, identity_document_number, audited_field) cnt\n" +
        "           FROM\n" +
        "                (SELECT\n" +
        "                         audited_action\n" +
        "                      ,old_value\n" +
        "                      ,ROW_NUMBER() OVER (PARTITION BY audited_action,audited_field,object_id,child_id ORDER BY S.id) rn\n" +
        "                      ,S.object_id\n" +
        "                      ,S.child_id\n" +
        "                      ,S.audited_field\n" +
        "                 FROM\n" +
        "                      version_audits S\n" +
        "                 WHERE\n" +
        "                     S.audited_action IN ('RECORD_IDENTITY_DOCUMENT_ADDITION','RECORD_IDENTITY_DOCUMENT_MODIFICATION','RECORD_IDENTITY_DOCUMENT_DELETION')\n" +
        "                   AND (S.audited_field = 'RECORD_IDENTITY_DOCUMENT_NUMBER'\n" +
        "                          OR S.audited_field IS NULL)) S\n" +
        "                  JOIN identity_document R ON S.child_id=R.id\n" +
        "                  JOIN ref_identity_document_type DT ON DT.id = R.identity_document_type_id\n" +
        "           WHERE\n" +
        "               S.rn=1\n" +
        "             AND DT.business_concept = 'IDT_PASSPORT') S\n" +
        "             where S.cnt < 2\n";

    String BIC_ADD_SUP_QUERY = " SELECT\n" +
        "            S.object,\n" +
        "            S.action,\n" +
        "            S.record_id,\n" +
        "            S.alias_reference,\n" +
        "            new_value,\n" +
        "            old_value,\n" +
        "            child_id,\n" +
        "            CASE WHEN action = 'ADD' THEN 'RECORD_CREATION' " +
        "                 ELSE 'RECORD_DELETION' END audited_action,\n" +
        "            'BIC' child_type,\n" +
            "            3 field_type_sort_priority\n" +
        "\n" +
        "     FROM\n" +
        "          (SELECT 'BIC' object,\n" +
        "                 audited_action action,\n" +
        "                 NULL alias_reference,\n" +
        "                 CASE WHEN audited_action = 'ADD' THEN  NULL\n" +
        "                      ELSE R.code\n" +
        "                      END  old_value,\n" +
        "                  CASE WHEN audited_action  = 'SUP' THEN NULL\n" +
        "                       ELSE\n" +
        "                      R.code\n" +
        "                      END  new_value,\n" +
        "                  S.audited_action,\n" +
        "                  S.record_id, \n" +
        "                  S.child_id\n" +
        "           FROM\n" +
        "                (\n" +
        "                SELECT\n" +
        "                       R.action audited_action\n" +
        "                    ,NULL old_value\n" +
        "                    ,RA.record_id\n" +
        "                    ,RA.bic_id child_id\n" +
        "                FROM\n" +
        "                     created_deleted_records_ids R\n" +
        "                       INNER JOIN record_bic RA ON RA.record_id = R.record_id) S\n" +
        "                  JOIN code_bic R ON S.child_id=R.id) S\n ";

    String BIC_MOD_QUERY = " SELECT\n" +
        "            S.object,\n" +
        "            'MOD'action,\n" +
        "            S.record_id,\n" +
        "            S.alias_reference,\n" +
        "            new_value,\n" +
        "            old_value,\n" +
        "            s.child_id,\n" +
        "            S.audited_action,\n" +
        "            'BIC' child_type,\n" +
        "              3 field_type_sort_priority\n" +
        "\n" +
        "     FROM\n" +
        "          (SELECT 'BIC' object,\n" +
        "                 audited_action action,\n" +
        "                 NULL alias_reference,\n" +
        "                 CASE WHEN audited_action = 'RECORD_BIC_ADDITION' THEN  NULL\n" +
        "                      WHEN audited_action = 'RECORD_BIC_MODIFICATION' THEN  COALESCE(s.old_value,R.code)\n" +
        "                      ELSE R.code\n" +
        "                     END  old_value,\n" +
        "                 CASE WHEN audited_action  = 'RECORD_BIC_DELETION' THEN NULL\n" +
        "                       ELSE\n" +
        "                       R.code\n" +
        "                       END  new_value,\n" +
        "                   S.audited_action,\n" +
        "                   S.object_id record_id,\n" +
        "                   S.child_id child_id,\n" +
        "                   count(*) over (partition by object_id, child_id, R.code , audited_field) cnt\n" +
        "           FROM\n" +
        "                (SELECT\n" +
        "                         audited_action\n" +
        "                      ,ROW_NUMBER() OVER (PARTITION BY audited_action,audited_field,object_id,child_id ORDER BY S.id) rn\n" +
        "                      ,S.old_value\n" +
        "                      ,S.object_id\n" +
        "                      ,S.child_id\n" +
        "                      ,S.audited_field\n" +
        "                 FROM\n" +
        "                      version_audits S\n" +
        "                 WHERE\n" +
        "                     S.audited_action IN ('RECORD_BIC_ADDITION','RECORD_BIC_DELETION','RECORD_BIC_MODIFICATION')\n" +
        "                   AND (S.audited_field = 'BIC_VALUE'\n" +
        "                          OR S.audited_field IS NULL)\n" +
        "                ) S\n" +
        "                  JOIN code_bic R ON S.child_id=R.id\n" +
        "           WHERE S.rn=1) S\n" +
        "           where S.cnt < 2\n ";

    String ISIN_ADD_SUP_QUERY = " SELECT\n" +
        "            S.object,\n" +
        "            S.action,\n" +
        "            S.record_id,\n" +
        "            S.alias_reference,\n" +
        "            new_value,\n" +
        "            old_value,\n" +
        "            child_id,\n" +
        "            CASE WHEN action = 'ADD' THEN 'RECORD_CREATION' " +
        "                 ELSE 'RECORD_DELETION' END audited_action,\n" +
        "            'ISIN' child_type,\n" +
        "              5 field_type_sort_priority\n" +
        "\n" +
        "     FROM\n" +
        "          (SELECT 'ISIN' object,\n" +
        "                  audited_action action,\n" +
        "                  NULL alias_reference,\n" +
        "                  CASE WHEN audited_action = 'ADD' THEN  NULL\n" +
        "                       ELSE R.code\n" +
        "                      END  old_value,\n" +
        "                  CASE WHEN audited_action  = 'SUP' THEN NULL\n" +
        "                       ELSE\n" +
        "                      R.code\n" +
        "                      END  new_value,\n" +
        "                  S.audited_action,\n" +
        "                  S.object_id record_id,\n" +
        "                  S.child_id child_id,\n" +
        "           FROM\n" +
        "                (\n" +
        "                SELECT\n" +
        "                       R.action audited_action\n" +
        "                    ,NULL old_value\n" +
        "                    ,RA.record_id object_id\n" +
        "                    ,RA.isin_id child_id\n" +
        "                FROM\n" +
        "                     created_deleted_records_ids R\n" +
        "                       INNER JOIN record_isin RA ON RA.record_id = R.record_id) S\n" +
        "                  JOIN code_isin R ON S.child_id=R.id) S\n ";

    String ISIN_MOD_QUERY = " SELECT\n" +
        "            S.object,\n" +
        "            S.action,\n" +
        "            S.record_id,\n" +
        "            S.alias_reference,\n" +
        "            new_value,\n" +
        "            old_value,\n" +
        "            s.child_id,\n" +
        "            S.audited_action,\n" +
        "            'ISIN' child_type,\n" +
        "            5 field_type_sort_priority\n" +
        "\n" +
        "     FROM\n" +
        "          (SELECT 'ISIN' object,\n" +
        "                 'MOD' action,\n" +
        "                 NULL alias_reference,\n" +
        "                 CASE WHEN audited_action = 'RECORD_ISIN_ADDITION' THEN  NULL\n" +
        "                       WHEN audited_action = 'RECORD_ISIN_MODIFICATION' THEN  COALESCE(s.old_value,R.code)\n" +
        "                       ELSE R.code\n" +
        "                      END  old_value,\n" +
        "                  CASE WHEN audited_action  = 'RECORD_ISIN_DELETION' THEN NULL\n" +
        "                       ELSE\n" +
        "                      R.code\n" +
        "                      END  new_value,\n" +
        "                  S.audited_action,\n" +
        "                  S.object_id record_id,\n" +
        "                  S.child_id child_id,\n" +
        "                  count(*) over (partition by object_id, child_id, R.code , audited_field) cnt\n" +
        "           FROM\n" +
        "                (SELECT\n" +
        "                        audited_action\n" +
        "                     ,ROW_NUMBER() OVER (PARTITION BY audited_action,audited_field,object_id,child_id ORDER BY S.id) rn\n" +
        "                     ,S.old_value\n" +
        "                     ,S.object_id\n" +
        "                     ,S.child_id\n" +
        "                     ,S.audited_field\n" +
        "                 FROM\n" +
        "                      version_audits S\n" +
        "                 WHERE\n" +
        "                    S.audited_action IN ('RECORD_ISIN_ADDITION','RECORD_ISIN_DELETION','RECORD_ISIN_MODIFICATION')\n" +
        "                  AND (S.audited_field = 'ISIN_VALUE'\n" +
        "                         OR S.audited_field IS NULL)\n" +
        "               ) S\n" +
        "                 JOIN code_isin R ON S.child_id=R.id" +
        "               WHERE S.rn=1" +
        ") S\n" +
        "where S.cnt < 2";


    String ADDRESS_MOD_QUERY = " SELECT\n" +
        "           S.object,\n" +
        "           S.action,\n" +
        "           S.record_id,\n" +
        "           S.alias_reference,\n" +
        "           new_value || CASE WHEN audited_action = 'RECORD_ADDRESS_DELETION' THEN ''\n" +
        "                            ELSE nvl(country_new, '')\n" +
        "               END new_value,\n" +
        "           old_value || CASE WHEN audited_action = 'RECORD_ADDRESS_ADDITION' THEN ''\n" +
        "                               ELSE nvl(country_old, '')\n" +
        "                END old_value,\n" +
        "            S.child_id,\n" +
        "            S.audited_action,\n" +
        "            'ADDRESS' child_type,\n" +
        "                8 field_type_sort_priority\n" +
        "\n" +
        "     FROM\n" +
        "          (SELECT 'ADDRESS' object,\n" +
        "                  'MOD' action,\n" +
        "                  null alias_reference,\n" +
        "                  CASE WHEN audited_action = 'RECORD_ADDRESS_ADDITION' THEN NULL\n" +
        "                       WHEN audited_action = 'RECORD_ADDRESS_DELETION' THEN    nvl(R.address, '')\n" +
        "                                                                                 || '@@@'\n" +
        "                                                                                 || nvl(R.zip_code, '')\n" +
        "                                                                                 || '@@@'\n" +
        "                                                                                 || nvl(R.city, '')\n" +
        "                                                                                 || '@@@'\n" +
            "                                                                             || nvl(R.state, '')\n" +
            "                                                                             || '@@@'\n" +
        "                                                                                 || nvl(c.label, '')\n" +
        "                                                                                 || '@@@'\n" +
        "                       ELSE nvl(s.address, '')\n" +
        "                              || '@@@'\n" +
        "                              || nvl(s.zip_code, '')\n" +
        "                              || '@@@'\n" +
        "                              || nvl(s.city, '')\n" +
        "                              || '@@@'\n" +
            "                             || nvl(s.state, '')\n" +
            "                             || '@@@'\n" +
        "                             || nvl(s.country, '')\n" +
        "                             || '@@@'\n" +
        "                     END  old_value,\n" +
        "                 CASE\n" +
        "                   WHEN audited_action = 'RECORD_ADDRESS_DELETION' THEN NULL\n" +
        "                   ELSE    nvl(R.address, '')\n" +
        "                             || '@@@'\n" +
        "                             || nvl(R.zip_code, '')\n" +
        "                             || '@@@'\n" +
        "                             || nvl(R.city, '')\n" +
        "                             || '@@@'\n" +
            "                         || nvl(R.state, '')\n" +
            "                         || '@@@'\n" +
        "                             || nvl(c.label, '')\n" +
        "                             || '@@@'\n" +
        "                     END  new_value,\n" +
        "                  S.old_value country_old,\n" +
        "                  S.new_value country_new,\n" +
        "                    S.audited_action,\n" +
        "                  S.object_id record_id,\n" +
        "                  S.child_id child_id,\n" +
        "                                count(*) over (partition by object_id, child_id, R.address|| '@@@'|| R.zip_code|| '@@@'|| R.city ||'@@@'|| R.state || '@@@'||R.country , audited_field) cnt\n" +
        "           FROM\n" +
        "                (SELECT\n" +
        "                        audited_action\n" +
        "                      ,object_id\n" +
        "                      ,child_id\n" +
        "                      ,MAX(CASE WHEN audited_field IS NULL OR audited_field='RECORD_ADDRESS_COUNTRY' THEN old_value ELSE NULL END) old_value\n" +
        "                      ,MAX(CASE WHEN audited_field IS NULL OR audited_field='RECORD_ADDRESS_COUNTRY' THEN new_value ELSE NULL END) new_value\n" +
        "                      ,MAX(CASE WHEN audited_action='RECORD_ADDRESS_MODIFICATION' THEN 1 ELSE NULL END) audited_field\n" +
        "                      ,MAX(CASE WHEN audited_field='RECORD_ADDRESS_ADDRESS' THEN old_value ELSE NULL END) address\n" +
        "                      ,MAX(CASE WHEN audited_field='RECORD_ADDRESS_ZIP_CODE' THEN old_value ELSE NULL END) zip_code\n" +
        "                      ,MAX(CASE WHEN audited_field='RECORD_ADDRESS_CITY' THEN old_value ELSE NULL END) city\n" +
        "                      ,MAX(CASE WHEN audited_field='RECORD_ADDRESS_COUNTRY' THEN old_value ELSE NULL END) country\n" +
            "                      ,MAX(CASE WHEN audited_field='RECORD_ADDRESS_STATE' THEN old_value ELSE NULL END) state\n" +

        "                FROM\n" +
        "                    (SELECT\n" +
        "                           *\n" +
        "                      FROM\n" +
        "                           (SELECT\n" +
        "                                  S.audited_action\n" +
        "                               ,S.audited_field\n" +
        "                               ,FIRST_VALUE(old_value) OVER (PARTITION BY audited_action,audited_field,object_id,child_id ORDER BY S.id ROWS BETWEEN UNBOUNDED " +
            "PRECEDING AND UNBOUNDED FOLLOWING) old_value\n" +
        "                               ,LAST_VALUE(new_value) OVER (PARTITION BY audited_action,audited_field,object_id,child_id ORDER BY S.id ROWS BETWEEN UNBOUNDED " +
            "PRECEDING AND UNBOUNDED FOLLOWING) new_value \n" +
        "                               ,ROW_NUMBER() OVER (PARTITION BY audited_action,audited_field,object_id,child_id ORDER BY S.id) rn\n" +
        "                               ,S.object_id\n" +
        "                               ,S.child_id\n" +
        "                            FROM\n" +
        "                                 version_audits S\n" +
        "                            WHERE\n" +
        "                                 (S.audited_action IN ('RECORD_ADDRESS_ADDITION','RECORD_ADDRESS_DELETION') AND S.audited_field IS NULL)\n" +
        "                                      OR\n" +
        "                                 (S.audited_action = 'RECORD_ADDRESS_MODIFICATION' and S.audited_field IN ( 'RECORD_ADDRESS_ADDRESS','RECORD_ADDRESS_ZIP_CODE'," +
            "'RECORD_ADDRESS_CITY','RECORD_ADDRESS_COUNTRY'))\n" +
        "                           ) WHERE rn=1) S\n" +
        "                GROUP BY\n" +
        "                         audited_action\n" +
        "                    ,object_id\n" +
        "                    ,child_id) S\n" +
        "                 JOIN address R ON S.child_id=R.id\n" +
        "                 LEFT OUTER JOIN ref_country C on R.country = c.id) S\n" +
        "           WHERE S.cnt < 2" ;

    String CITIZENSHIP_MOD_QUERY = " SELECT S.object,\n" +
        "       'MOD'  action,\n" +
        "       S.record_id,\n" +
        "       S.alias_reference,\n" +
        "       CASE\n" +
        "         WHEN audited_action = 'RECORD_COUNTRY_DETAILS_DELETION' THEN NULL\n" +
        "         ELSE S.new_value\n" +
        "           END new_value,\n" +
        "       CASE\n" +
        "         WHEN audited_action = 'RECORD_COUNTRY_DETAILS_ADDITION' THEN NULL\n" +
        "         ELSE S.old_value\n" +
        "           END old_value,\n" +
        "            S.child_id,\n" +
        "            S.audited_action,\n" +
        "            'COUNTRY_DETAILS' child_type,\n" +
        "           9 field_type_sort_priority\n" +
        "\n" +
        "FROM (SELECT 'CITIZENSHIP' object,\n" +
        "            NULL          alias_reference,\n" +
        "            S.audited_action,\n" +
        "            S.object_id   record_id,\n" +
        "            S.old_value,\n" +
        "            S.new_value,\n" +
        "            r.country_id,\n" +
        "            S.child_id,\n" +
        "            count(*) over (partition by object_id, child_id, R.country_id , audited_field) cnt\n" +
        "     FROM  (SELECT\n" +
        "                       audited_action\n" +
        "                      ,FIRST_VALUE(old_value) OVER (PARTITION BY audited_action,audited_field,object_id,child_id ORDER BY S.id ROWS BETWEEN UNBOUNDED PRECEDING " +
            "AND UNBOUNDED FOLLOWING) old_value\n" +
        "                      ,LAST_VALUE(new_value) OVER (PARTITION BY audited_action,audited_field,object_id,child_id ORDER BY S.id ROWS BETWEEN UNBOUNDED PRECEDING " +
            "AND UNBOUNDED FOLLOWING) new_value \n" +
        "                      ,ROW_NUMBER() OVER (PARTITION BY audited_action,audited_field,object_id,child_id ORDER BY S.id) rn\n" +
        "                      ,S.object_id\n" +
        "                      ,S.child_id\n" +
        "                      ,S.audited_field\n" +
        "                  FROM\n" +
        "                       version_audits S\n" +
        "                  WHERE\n" +
        "                      S.audited_action IN ('RECORD_COUNTRY_DETAILS_ADDITION','RECORD_COUNTRY_DETAILS_DELETION','RECORD_COUNTRY_DETAILS_MODIFICATION')\n" +
        "                    AND (S.audited_field = 'RECORD_COUNTRY_DETAILS_BIRTH_COUNTRY'\n" +
        "                           OR S.audited_field IS NULL)) S\n" +
        "             JOIN country_details R ON S.child_id = R.id\n" +
        "             JOIN ref_country_type ct ON R.country_type_id = CT.id\n" +
        "               WHERE   S.rn=1 AND" +
        "               CT.business_concept = 'COUNTRY_TYPE_CITIZENSHIP') S\n" +
        "        WHERE S.cnt < 2";

    String DATE_AND_PLACE_OF_BIRTH_MOD_QUERY = " SELECT\n" +
        "           S.object,\n" +
        "           'MOD' action,\n" +
        "           S.record_id,\n" +
        "           S.alias_reference,\n" +
        "           new_value,\n" +
        "           old_value,\n" +
        "           S.child_id,\n" +
        "           S.audited_action,\n" +
        "           'PERSON_BIOGRAPHY' child_type,\n" +
        "           DECODE(object, 'DATE_OF_BIRTH', 6, 7) field_type_sort_priority\n" +
        "\n" +
        "     FROM\n" +
        "          (SELECT  CASE\n" +
        "                    WHEN COALESCE(s.date_of_birth, R.date_value) IS NOT NULL THEN 'DATE_OF_BIRTH'\n" +
        "                    ELSE 'PLACE_OF_BIRTH'\n" +
        "                     END object,\n" +
        "                 null alias_reference,\n" +
        "                 CASE WHEN audited_action = 'RECORD_PERSON_BIOGRAPHY_ADDITION' THEN NULL\n" +
        "                      WHEN audited_action = 'RECORD_PERSON_BIOGRAPHY_DELETION' THEN    coalesce(R.date_value, R.birth_place, '')\n" +
        "                        ELSE COALESCE(" +
        "                                       COALESCE(s.date_of_birth, R.date_value), COALESCE(s.place_of_birth, R.birth_place), ''" +
        "                               )\n" +
        "                       END  old_value,\n" +
        "                   CASE\n" +
        "                     WHEN audited_action = 'RECORD_PERSON_BIOGRAPHY_DELETION' THEN NULL\n" +
        "                     ELSE   COALESCE(R.date_value, R.birth_place, '')\n" +
        "                       END  new_value,\n" +
        "                   S.audited_action,\n" +
        "                   S.object_id record_id,\n" +
        "                   S.child_id child_id,\n" +
        "                   count(*) over (partition by object_id, child_id, COALESCE(R.date_value, R.birth_place) , audited_field) cnt\n" +
        "           FROM\n" +
        "                (SELECT\n" +
        "                       audited_action\n" +
        "                    ,object_id\n" +
        "                    ,child_id\n" +
        "                     ,MAX(CASE WHEN audited_action='RECORD_PERSON_BIOGRAPHY_MODIFICATION' THEN 1 ELSE NULL END) audited_field\n" +
        "                     ,MAX(CASE WHEN audited_field='RECORD_PERSON_BIOGRAPHY_DATE_VALUE' THEN old_value ELSE NULL END) date_of_birth\n" +
        "                     ,MAX(CASE WHEN audited_field='RECORD_PERSON_BIOGRAPHY_BIRTH_PLACE' THEN old_value ELSE NULL END) place_of_birth\n" +
        "                FROM\n" +
        "                                          (SELECT\n" +
        "                          *\n" +
        "                      FROM\n" +
        "                           (SELECT\n" +
        "                                  S.audited_action\n" +
        "                                ,S.audited_field\n" +
        "                                ,old_value\n" +
        "                                ,ROW_NUMBER() OVER (PARTITION BY audited_action,audited_field,object_id,child_id ORDER BY S.id) rn\n" +
        "                                ,S.object_id\n" +
        "                                ,S.child_id\n" +
        "                            FROM\n" +
        "                                 version_audits S\n" +
        "                                 JOIN person_biography R ON S.child_id=R.id\n" +
        "                                 LEFT JOIN ref_date_type DT ON  R.date_type_id = DT.id\n" +
        "                            WHERE\n" +
        "                                S.audited_action IN ('RECORD_PERSON_BIOGRAPHY_MODIFICATION','RECORD_PERSON_BIOGRAPHY_ADDITION','RECORD_PERSON_BIOGRAPHY_DELETION')\n" +
        "                              AND (S.audited_field IN ( 'RECORD_PERSON_BIOGRAPHY_DATE_VALUE','RECORD_PERSON_BIOGRAPHY_BIRTH_PLACE')\n" +
        "                                    OR S.audited_field IS NULL)\n" +
        "                              AND (DT.business_concept = 'DATE_TYPE_DATE_OF_BIRTH' OR R.date_type_id IS NULL)\n" +
        "                           ) WHERE rn=1) S\n" +
        "                GROUP BY\n" +
        "                         audited_action\n" +
        "                    ,object_id\n" +
        "                    ,child_id) S\n" +
        "                 JOIN person_biography R ON S.child_id=R.id\n" +
        "                 LEFT JOIN ref_date_type DT ON R.date_type_id = DT.id\n AND dt.business_concept = 'DATE_TYPE_DATE_OF_BIRTH'\n" +
        "         ) S\n " +
        "         WHERE S.cnt <2 \n";

    String COUNT_MAIN_QUERY = " ( " +
        PRIMARY_NAME_ADD_SUP_QUERY +
        " UNION ALL " + PRIMARY_NAME_MOD_QUERY + //NOSONAR
        " UNION ALL " + ALIAS_ADD_SUP_QUERY + //NOSONAR
        " UNION ALL " + ALIAS_MOD_QUERY + //NOSONAR
        " UNION ALL " + PASSPORT_ADD_SUP_QUERY + //NOSONAR
        " UNION ALL " + PASSPORT_MOD_QUERY + //NOSONAR
        " UNION ALL " + BIC_ADD_SUP_QUERY + //NOSONAR
        " UNION ALL " + BIC_MOD_QUERY + //NOSONAR
        " UNION ALL " + ISIN_ADD_SUP_QUERY + //NOSONAR
        " UNION ALL " + ISIN_MOD_QUERY + //NOSONAR
        " ) ";

    String COUNT_ALIAS_MAIN_QUERY = " ( " + ALIAS_ADD_SUP_QUERY + //NOSONAR
            " UNION ALL " + ALIAS_MOD_QUERY + //NOSONAR

            " ) ";

    /*For MOD table we have to include all records that had one of 8 fields modified:
     * Primary Name, Alias, Passport, ISIN, BIC Code, address, citizenship, Date/Place of birth.
     * We need a separate query because Add and Sup tables only count records that had the first 5 fields
     * in the list above modified*/
    String COUNT_MODIFIED_RECORDS_QUERY = " ( " +
        PRIMARY_NAME_MOD_QUERY +
        " UNION ALL " + ALIAS_MOD_QUERY + //NOSONAR
        " UNION ALL " + PASSPORT_MOD_QUERY + //NOSONAR
        " UNION ALL " + BIC_MOD_QUERY + //NOSONAR
        " UNION ALL " + ISIN_MOD_QUERY + //NOSONAR
        " UNION ALL " + ADDRESS_MOD_QUERY + //NOSONAR
        " UNION ALL " + CITIZENSHIP_MOD_QUERY + //NOSONAR
        " UNION ALL " + DATE_AND_PLACE_OF_BIRTH_MOD_QUERY + //NOSONAR
        " ) ";

    String DETAILS_MAIN_ADD_SUP_QUERY = " ( " +
        PRIMARY_NAME_ADD_SUP_QUERY +
        " UNION ALL " + ALIAS_ADD_SUP_QUERY + //NOSONAR
        " UNION ALL " + PASSPORT_ADD_SUP_QUERY + //NOSONAR
        " UNION ALL " + BIC_ADD_SUP_QUERY + //NOSONAR
        " UNION ALL " + ISIN_ADD_SUP_QUERY + //NOSONAR
        " ) ";

    String DETAILS_MAIN_MOD_QUERY = " ( " +
        PRIMARY_NAME_MOD_QUERY +
        " UNION ALL " + ALIAS_MOD_QUERY + //NOSONAR
        " UNION ALL " + PASSPORT_MOD_QUERY + //NOSONAR
        " UNION ALL " + BIC_MOD_QUERY + //NOSONAR
        " UNION ALL " + ISIN_MOD_QUERY + //NOSONAR
        " UNION ALL " + ADDRESS_MOD_QUERY + //NOSONAR
        " UNION ALL " + CITIZENSHIP_MOD_QUERY + //NOSONAR
        " UNION ALL " + DATE_AND_PLACE_OF_BIRTH_MOD_QUERY + //NOSONAR
        " ) ";

    String DETAILS_MAIN_QUERY = " ( " +
        DETAILS_MAIN_MOD_QUERY +
        " UNION ALL " + DETAILS_MAIN_ADD_SUP_QUERY + //NOSONAR
        " ) ";

    String SELECT_FOR_QUERY_BY_ACTION = " SELECT all_data.action, " +
            "      (SELECT START_DATE FROM VERSION WHERE ID = :versionId) OFFICIAL_DATE, " +
            "      r.id id, " +
            "      r.RECORD_TYPE TYPE_FOR_SORT, " +
            "      r.REGLISS_REFERENCE, " +
            "      all_data.alias_reference AKA_REFERENCE, " +
            "      r.RECORD_ID, " +
            "      trim(all_data.object) ELEMENT_FOR_SORT, " +
            "      nvl(r.FIRST_NAME,'') || '@@@' || nvl(r.MIDDLE_NAME,'') || '@@@' || nvl(r.LAST_NAME,'') || '@@@' || nvl(r.NAME,'') PRIMARY_NAME, " +
            "      all_data.new_value, " +
            "      all_data.old_value, " +
            "      all_data.child_id, " +
            "      all_data.audited_action, " +
            "      all_data.child_type, ";

    String SORT_COLUMNS_FOR_QUERY_BY_ACTION = " TRIM(UPPER(CASE WHEN r.RECORD_TYPE = 'PERSON' THEN nvl(r.FIRST_NAME,'') || ' ' || nvl(r.MIDDLE_NAME,'') || ' ' || nvl(r.LAST_NAME,'') " +
            "           ELSE nvl(r.NAME,'') END)) PRIMARY_NAME_FOR_SORT, " +
            "      COALESCE(all_data.alias_reference, r.REGLISS_REFERENCE) REFERENCE_FOR_SORT ";

    String SORT_COLUMNS_FOR_QUERY_BY_ACTION_WITH_VERIFICATION_INFO =
            " , (CASE \n" +
            "          WHEN rvc.record_id IS NULL THEN \n" +
            "                   CASE WHEN rvi.verification_status is NULL THEN 0 \n" +
            "                        WHEN rvi.verification_status = 'KO' THEN 1 \n" +
            "                        WHEN rvi.verification_status = 'OK' THEN 2 \n" +
            "                   END\n" +
            "          WHEN rvc.record_id IS NOT NULL THEN \n" +
            "                   CASE WHEN NVL(rvi.verification_status,' ') =  NVL(rvc.verification_status,' ') THEN \n" +
            "                            CASE WHEN rvi.verification_status is NULL THEN 0 \n" +
            "                                 WHEN rvi.verification_status = 'KO' THEN 1 \n" +
            "                                 WHEN rvi.verification_status = 'OK' THEN 2 \n" +
            "                            END \n" +
            "                   WHEN NVL(rvi.verification_status,' ') <>  NVL(rvc.verification_status,' ') THEN \n" +
            "                            CASE WHEN rvc.verification_status is NULL THEN 0 \n" +
            "                                 WHEN rvc.verification_status = 'KO' THEN 1 \n" +
            "                                 WHEN rvc.verification_status = 'OK' THEN 2 \n" +
            "                            END \n" +
            "                   END \n" +
            "       END)  VERIFICATION_STATUS_FOR_SORT ";



    String JOIN_RECORD_VERIFICATION_INFO = " LEFT JOIN APP_RECORD_VERIFICATION_INFO rvi " +
            " ON rvi.record_id = r.id AND nvl(rvi.child_id, 0) = nvl(all_data.child_id, 0) " +
            " AND nvl(rvi.child_type, 'RECORD') = nvl(all_data.child_type, 'RECORD') " +
            " AND rvi.audited_action = all_data.audited_action AND rvi.version_id = :versionId ";

    String JOIN_RECORD_VERIFICATION_CHANGES = " LEFT JOIN APP_RECORD_VERIF_CHANGES rvc " +
            " ON rvc.record_id = r.id AND nvl(rvc.child_id, 0) = nvl(all_data.child_id, 0) " +
            " AND nvl(rvc.child_type, 'RECORD') = nvl(all_data.child_type, 'RECORD') " +
            " AND rvc.audited_action = all_data.audited_action " +
            " AND rvc.version_id = :versionId ";

    String ADD_SUP_QUERY_BY_ACTION_WITH_VERIFICATION_INFO =  MAIN_QUERY_WITH_CLAUSE +
            SELECT_FOR_QUERY_BY_ACTION +
            " r.external_uuid, " +
            " rvi.verification_comment as verification_comment, " +
            " rvi.verification_status as verification_status, " +
            " u.username as user_uid, " +
            " rvi.save_date as save_date, " +
            " rvc.verification_comment as temporaryVerification_comment, " +
            " rvc.verification_status as temporaryVerification_status, " +
            " rvc.record_id as temporaryRecordId, " +
            //keep the columns for sort on the last positions, as they must not be mapped to the VO returned
            SORT_COLUMNS_FOR_QUERY_BY_ACTION +
            SORT_COLUMNS_FOR_QUERY_BY_ACTION_WITH_VERIFICATION_INFO +
            " FROM " + DETAILS_MAIN_ADD_SUP_QUERY + " all_data" +
            " INNER JOIN RECORD r on r.id = all_data.record_id " +
            JOIN_RECORD_VERIFICATION_INFO +
            JOIN_RECORD_VERIFICATION_CHANGES +
            "LEFT JOIN USERS u on u.id = rvi.user_id " +
            " WHERE all_data.action = :action ";

    String MOD_QUERY_BY_ACTION_WITH_VERIFICATION_INFO =  MAIN_QUERY_WITH_CLAUSE +
            SELECT_FOR_QUERY_BY_ACTION +
            " r.external_uuid, " +
            " rvi.verification_comment as verification_comment, " +
            " rvi.verification_status as verification_status, " +
            " u.username as user_uid, " +
            " rvi.save_date as save_date, " +
            " rvc.verification_comment as temporaryVerification_comment, " +
            " rvc.verification_status as temporaryVerification_status, " +
            " rvc.record_id as temporaryRecordId, " +
            //keep the columns for sort on the last positions, as they must not be mapped to the VO returned
            SORT_COLUMNS_FOR_QUERY_BY_ACTION +
            SORT_COLUMNS_FOR_QUERY_BY_ACTION_WITH_VERIFICATION_INFO +
            " FROM " + DETAILS_MAIN_MOD_QUERY + " all_data" +
            " INNER JOIN RECORD r on r.id = all_data.record_id " +
            JOIN_RECORD_VERIFICATION_INFO +
            JOIN_RECORD_VERIFICATION_CHANGES +
            "LEFT JOIN USERS u on u.id = rvi.user_id ";

    @Query(nativeQuery = true,
        value = MAIN_QUERY_WITH_CLAUSE +
                   "SELECT trim(object), action, count(*) FROM (" + " SELECT CASE WHEN all_data.object like 'ALIAS%'\n "
                   + "              THEN 'ALIAS'\n " +
                   + "              ELSE all_data.object END object, all_data.action \n"
                   + " FROM " + COUNT_MAIN_QUERY + " all_data)" + " group by object, action ")
    List<Object[]> getAddModSupElementsCountsRaw(@Param("versionId") Long versionId);

    @Query(nativeQuery = true,
            value = MAIN_QUERY_WITH_CLAUSE +
                    "SELECT trim(object), action, count(*) FROM (" + " SELECT 'ALIAS' object, all_data.action \n"
                    + " FROM " + COUNT_ALIAS_MAIN_QUERY + " all_data)" + " group by object, action ")
    List<Object[]> getAddModSupAliasCountsRaw(@Param("versionId") Long versionId);


    @Query(nativeQuery = true,
        value = MAIN_QUERY_WITH_CLAUSE +
            " SELECT all_data.action, count(*) " +
            " FROM " + DETAILS_MAIN_QUERY + " all_data" +
            " group by all_data.action ")
    List<Object[]> getAddModSupDetailsElementsCountsRaw(@Param("versionId") Long versionId);

    @Query(nativeQuery = true,
        value = MAIN_QUERY_WITH_CLAUSE +
            " SELECT r.RECORD_TYPE, all_data.action, count(distinct all_data.record_id) "+
            " FROM " + COUNT_MAIN_QUERY + " all_data" +
            " INNER JOIN record r on r.id = all_data.record_id " +
            " group by r.RECORD_TYPE, all_data.action ")
    List<Object[]> getAddModSupRecordsCountRaw(@Param("versionId") Long versionId);

    @Query(nativeQuery = true,
        value = MAIN_QUERY_WITH_CLAUSE +
            " SELECT case when count(distinct all_data.record_id) is null then 0 else count(distinct all_data.record_id) end"+
            " FROM " + COUNT_MODIFIED_RECORDS_QUERY + " all_data\n"
    )
    int countModifiedRecordsForVersionSynthesis(@Param("versionId") Long versionId);

    @Query(nativeQuery = true,
        value = MAIN_QUERY_WITH_CLAUSE +
               SELECT_FOR_QUERY_BY_ACTION +
               //keep the columns for sort on the last positions, as they must not be mapped to the VO returned
               SORT_COLUMNS_FOR_QUERY_BY_ACTION +
               " FROM " + DETAILS_MAIN_ADD_SUP_QUERY + " all_data" +
               " INNER JOIN RECORD r on r.id = all_data.record_id " +
               " WHERE all_data.action = :action " +
            " ORDER BY REGLISS_REFERENCE, FIELD_TYPE_SORT_PRIORITY ,AKA_REFERENCE ")
    Stream<Object[]> getAddSupElementsByActionRaw(@Param("versionId") Long versionId, @Param("action") String action);

    @Query(nativeQuery = true,
            value = MAIN_QUERY_WITH_CLAUSE +
                    " SELECT all_data.RECORD_ID " +
                    " FROM " + DETAILS_MAIN_ADD_SUP_QUERY + " all_data" +
                    " WHERE all_data.action = :action ")
    Stream<BigDecimal> getRecordIdsForSynthesisOfAddSupElementsByActionRaw(@Param("versionId") Long versionId, @Param("action") String action);

    @Query(nativeQuery = true,
        value = MAIN_QUERY_WITH_CLAUSE +
                SELECT_FOR_QUERY_BY_ACTION +
                //keep the columns for sort on the last positions, as they must not be mapped to the VO returned
                SORT_COLUMNS_FOR_QUERY_BY_ACTION +
                " FROM " + DETAILS_MAIN_MOD_QUERY + " all_data" +
                " INNER JOIN RECORD r on r.id = all_data.record_id " +
            " ORDER BY REGLISS_REFERENCE, FIELD_TYPE_SORT_PRIORITY, AKA_REFERENCE ")
    Stream<Object[]> getModElementsRaw(@Param("versionId") Long versionId);

    @Query(nativeQuery = true,
            value = MAIN_QUERY_WITH_CLAUSE +
                    " SELECT all_data.RECORD_ID " +
                    " FROM " + DETAILS_MAIN_MOD_QUERY + " all_data")
    Stream<BigDecimal> getRecordIdsForSynthesisOfModElementsRaw(@Param("versionId") Long versionId);

    @Query(nativeQuery = true,
        value = MAIN_QUERY_WITH_CLAUSE +
            " SELECT all_data.action, " +
            "       (SELECT START_DATE FROM VERSION WHERE ID = :versionId) OFFICIAL_DATE, " +
            "       r.id id, " +
            "       r.RECORD_TYPE, " +
            "       r.REGLISS_REFERENCE, " +
            "       all_data.alias_reference AKA_REFERENCE, " +
            "       r.RECORD_ID, " +
            "       trim(all_data.object), " +
            "       nvl(r.FIRST_NAME,'') || '@@@' || nvl(r.MIDDLE_NAME,'') || '@@@' || nvl(r.LAST_NAME,'') || '@@@' || nvl(r.NAME,'') || '@@@' || nvl(r.SUFFIX,'') PRIMARY_NAME, " +
            "       all_data.new_value," +
            "       all_data.old_value, " +
            "       all_data.child_id, " +
            "       all_data.audited_action, " +
            "       all_data.child_type " +
                " FROM " + DETAILS_MAIN_QUERY + " all_data" +
            " INNER JOIN RECORD r on r.id = all_data.record_id " +
            " ORDER BY action, r.RECORD_ID, AKA_REFERENCE")
    Stream<Object[]> getAddModSupElementsRaw(@Param("versionId") Long versionId);

    @Query(nativeQuery = true,
    value="with version_audits as (SELECT /*+ materialize */\n" +
            "\t\t         A.id\n" +
            "\t\t         ,A.audited_action\n" +
            "\t\t         ,A.audited_field\n" +
            "\t\t         ,A.object_id\n" +
            "\t\t         ,A.child_id,\n" +
            "    A.part_version_id\n" +
            "\t\t     FROM\n" +
            "\t\t          audits A\n" +
            "\t\t     WHERE\n" +
            "\t\t         A.object_id in( select r1.id from record r1 where r1.regliss_reference=:reglissReference and r1.list_id=:listId) )\n" +
            "select Max(part_version_id) from\n" +
            "(\n" +
            "     --PRIMARY_NAME_MOD_QUERY\n" +
            "     SELECT  S.part_version_id\n" +
            "                                FROM version_audits S\n" +
            "                                WHERE\n" +
            "                                    S.audited_action ='RECORD_MODIFICATION'\n" +
            "                                    AND S.audited_field IN ( 'PERSON_FIRST_NAME','PERSON_LAST_NAME','PERSON_MIDDLE_NAME','RECORD_NAME')\n" +
            "    union all                 \n" +
            "    --ALIAS_MOD_QUERY\n" +
            "    SELECT  S.part_version_id\n" +
            "                                FROM version_audits S\n" +
            "                                WHERE\n" +
            "                                    S.audited_action IN ('RECORD_ALIAS_MODIFICATION','RECORD_ALIAS_ADDITION','RECORD_ALIAS_DELETION')\n" +
            "                                    AND (S.audited_field IN ( 'RECORD_ALIAS_FIRST_NAME','RECORD_ALIAS_LAST_NAME','RECORD_ALIAS_MIDDLE_NAME','RECORD_ALIAS_FULL_NAME'," +
            "'RECORD_ALIAS_SUFFIX')\n" +
            "                                    OR S.audited_field IS NULL)\n" +
            "    union all\n" +
            "    --PASSPORT_MOD_QUERY\n" +
            "    SELECT  S.part_version_id\n" +
            "                                FROM version_audits S\n" +
            "                                JOIN identity_document R ON S.child_id=R.id\n" +
            "                                JOIN ref_identity_document_type DT ON DT.id = R.identity_document_type_id AND DT.business_concept = 'IDT_PASSPORT'\n" +
            "                                WHERE\n" +
            "                                    S.audited_action IN ('RECORD_IDENTITY_DOCUMENT_ADDITION','RECORD_IDENTITY_DOCUMENT_MODIFICATION','RECORD_IDENTITY_DOCUMENT_DELETION')\n" +
            "                                    AND (S.audited_field = 'RECORD_IDENTITY_DOCUMENT_NUMBER'\n" +
            "                                    OR S.audited_field IS NULL)\n" +
            "    union all         \n" +
            "    -- BIC_MOD_QUERY\n" +
            "    SELECT  S.part_version_id\n" +
            "                            FROM version_audits S\n" +
            "                            WHERE\n" +
            "                                S.audited_action IN ('RECORD_BIC_ADDITION','RECORD_BIC_DELETION','RECORD_BIC_MODIFICATION')\n" +
            "                                AND (S.audited_field = 'BIC_VALUE'\n" +
            "                                OR S.audited_field IS NULL)\n" +
            "    union all\n" +
            "    --ISIN_MOD_QUERY\n" +
            "    SELECT  S.part_version_id\n" +
            "                            FROM version_audits S\n" +
            "                            WHERE\n" +
            "                                S.audited_action IN ('RECORD_ISIN_ADDITION','RECORD_ISIN_DELETION','RECORD_ISIN_MODIFICATION')\n" +
            "                                AND (S.audited_field = 'ISIN_VALUE'\n" +
            "                                OR S.audited_field IS NULL)\n" +
            "    union all\n" +
            "     --ADDRESS_MOD_QUERY\n" +
            "     SELECT  S.part_version_id\n" +
            "                           FROM version_audits S\n" +
            "                           JOIN address R ON S.child_id=R.id\n" +
            "                           WHERE\n" +
            "                               (S.audited_action IN ('RECORD_ADDRESS_ADDITION','RECORD_ADDRESS_DELETION') AND S.audited_field IS NULL)\n" +
            "                               OR\n" +
            "                               (S.audited_action = 'RECORD_ADDRESS_MODIFICATION' and S.audited_field IN ( 'RECORD_ADDRESS_ADDRESS','RECORD_ADDRESS_ZIP_CODE'," +
            "'RECORD_ADDRESS_CITY','RECORD_ADDRESS_COUNTRY','RECORD_ADDRESS_STATE'))\n" +
            "     union  all \n" +
            "     --CITIZENSHIP_MOD_QUERY\n" +
            "     SELECT  S.part_version_id\n" +
            "                            FROM version_audits S\n" +
            "                            JOIN country_details R ON S.child_id = R.id\n" +
            "                            JOIN ref_country_type ct ON R.country_type_id = CT.id and CT.business_concept = 'COUNTRY_TYPE_CITIZENSHIP'\n" +
            "                            WHERE\n" +
            "                                S.audited_action IN ('RECORD_COUNTRY_DETAILS_ADDITION','RECORD_COUNTRY_DETAILS_DELETION','RECORD_COUNTRY_DETAILS_MODIFICATION')\n" +
            "                                AND (S.audited_field = 'RECORD_COUNTRY_DETAILS_BIRTH_COUNTRY'\n" +
            "                                OR S.audited_field IS NULL)\n" +
            "    union all\n" +
            "    --DATE_AND_PLACE_OF_BIRTH_MOD_QUERY\n" +
            "    SELECT  S.part_version_id       \n" +
            "                            FROM version_audits S\n" +
            "                            JOIN person_biography R ON S.child_id=R.id\n" +
            "                            LEFT JOIN ref_date_type DT ON R.date_type_id = DT.id AND dt.business_concept = 'DATE_TYPE_DATE_OF_BIRTH'\n" +
            "                            WHERE\n" +
            "                                S.audited_action IN ('RECORD_PERSON_BIOGRAPHY_MODIFICATION','RECORD_PERSON_BIOGRAPHY_ADDITION','RECORD_PERSON_BIOGRAPHY_DELETION')\n" +
            "                                AND (S.audited_field IN ( 'RECORD_PERSON_BIOGRAPHY_DATE_VALUE','RECORD_PERSON_BIOGRAPHY_BIRTH_PLACE')\n" +
            "                                OR S.audited_field IS NULL)\n" +
            ")")
    Optional<Number> getLastVersionWithAddModSupForRecord(@Param("reglissReference") String reglissReference, @Param("listId") Long listId);

    @Query(nativeQuery = true,value = "WITH version_audits AS (SELECT /* materialize */A.id, A.audited_action, " +
            "A.audited_field, A.object_id, A.child_id," +
            " A.part_version_id" +
            " FROM audits A" +
            " WHERE A.PART_VERSION_ID = :VersionId )," +
            " max_part_version_id AS (" +
            " SELECT (OBJECT_ID) AS max_part_version_id" +
            " FROM (" +
            " SELECT S.OBJECT_ID" +
            " FROM version_audits S" +
            " WHERE S.audited_action = 'RECORD_MODIFICATION'" +
            " AND S.audited_field IN ('PERSON_FIRST_NAME', 'PERSON_LAST_NAME', 'PERSON_MIDDLE_NAME', 'RECORD_NAME')" +
            " UNION ALL" +
            " SELECT S.OBJECT_ID" +
            " FROM version_audits S" +
            " WHERE S.audited_action IN ('RECORD_ALIAS_MODIFICATION', 'RECORD_ALIAS_ADDITION', 'RECORD_ALIAS_DELETION')" +
            " AND (S.audited_field IN ('RECORD_ALIAS_FIRST_NAME', 'RECORD_ALIAS_LAST_NAME', 'RECORD_ALIAS_MIDDLE_NAME'," +
            " 'RECORD_ALIAS_FULL_NAME', 'RECORD_ALIAS_SUFFIX') OR S.audited_field IS NULL)" +
            " UNION ALL" +
            " SELECT S.OBJECT_ID" +
            " FROM version_audits S" +
            " JOIN identity_document R ON S.child_id = R.id" +
            " JOIN ref_identity_document_type DT ON DT.id = R.identity_document_type_id AND DT.business_concept = 'IDT_PASSPORT'" +
            " WHERE S.audited_action IN ('RECORD_IDENTITY_DOCUMENT_ADDITION', 'RECORD_IDENTITY_DOCUMENT_MODIFICATION'," +
            " 'RECORD_IDENTITY_DOCUMENT_DELETION')" +
            " AND (S.audited_field = 'RECORD_IDENTITY_DOCUMENT_NUMBER' OR S.audited_field IS NULL)" +
            " UNION ALL" +
            " SELECT S.OBJECT_ID" +
            " FROM version_audits S" +
            " WHERE S.audited_action IN ('RECORD_BIC_ADDITION', 'RECORD_BIC_DELETION', 'RECORD_BIC_MODIFICATION')" +
            " AND (S.audited_field = 'BIC_VALUE' OR S.audited_field IS NULL)" +
            " UNION ALL" +
            " SELECT S.OBJECT_ID" +
            " FROM version_audits S" +
            " WHERE S.audited_action IN ('RECORD_ISIN_ADDITION', 'RECORD_ISIN_DELETION', 'RECORD_ISIN_MODIFICATION')" +
            " AND (S.audited_field = 'ISIN_VALUE' OR S.audited_field IS NULL)" +
            " UNION ALL" +
            " SELECT S.OBJECT_ID" +
            " FROM version_audits S" +
            " JOIN address R ON S.child_id = R.id" +
            " WHERE (S.audited_action IN ('RECORD_ADDRESS_ADDITION', 'RECORD_ADDRESS_DELETION') AND S.audited_field IS NULL)" +
            " OR (S.audited_action = 'RECORD_ADDRESS_MODIFICATION' AND S.audited_field IN" +
            " ('RECORD_ADDRESS_ADDRESS', 'RECORD_ADDRESS_ZIP_CODE', 'RECORD_ADDRESS_CITY','RECORD_ADDRESS_STATE', 'RECORD_ADDRESS_COUNTRY','RECORD_ADDRESS_STATE'))" +
            " UNION ALL" +
            " SELECT S.OBJECT_ID" +
            " FROM version_audits S" +
            " JOIN country_details R ON S.child_id = R.id" +
            " JOIN ref_country_type CT ON R.country_type_id = CT.id AND CT.business_concept = 'COUNTRY_TYPE_CITIZENSHIP'" +
            " WHERE S.audited_action IN ('RECORD_COUNTRY_DETAILS_ADDITION', 'RECORD_COUNTRY_DETAILS_DELETION'," +
            " 'RECORD_COUNTRY_DETAILS_MODIFICATION')" +
            " AND (S.audited_field = 'RECORD_COUNTRY_DETAILS_BIRTH_COUNTRY' OR S.audited_field IS NULL)" +
            " UNION ALL" +
            " SELECT S.OBJECT_ID" +
            " FROM version_audits S" +
            " JOIN person_biography R ON S.child_id = R.id" +
            " LEFT JOIN ref_date_type DT ON R.date_type_id = DT.id AND DT.business_concept = 'DATE_TYPE_DATE_OF_BIRTH'" +
            " WHERE S.audited_action IN ('RECORD_PERSON_BIOGRAPHY_MODIFICATION', 'RECORD_PERSON_BIOGRAPHY_ADDITION'," +
            " 'RECORD_PERSON_BIOGRAPHY_DELETION')" +
            " AND (S.audited_field IN ('RECORD_PERSON_BIOGRAPHY_DATE_VALUE', 'RECORD_PERSON_BIOGRAPHY_BIRTH_PLACE')" +
            " OR S.audited_field IS NULL)" +
            " ))" +
            " SELECT DISTINCT(r.id)" +
            " FROM RECORD r" +
            " JOIN VERSION v ON v.id = :VersionId" +
            " JOIN max_part_version_id mpv ON r.id in( mpv.max_part_version_id)where r.LIST_ID = :listId")
    List<String> findListOfReglissreferences(@Param("VersionId") Long versionID, @Param("listId") Long listId);
}
