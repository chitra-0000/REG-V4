package com.bnpp.regliss.utils;

import com.bnpp.regliss.entity.record.EntityRecord;
import com.bnpp.regliss.entity.record.PersonRecord;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.record.VesselRecord;
import com.bnpp.regliss.entity.referential.record.RecordType;

public class NullObjectUtils {

    private static PersonRecord personRecord;
    private static EntityRecord entityRecord;
    private static VesselRecord vesselRecord;

    private NullObjectUtils() {
        //
    }

    private static PersonRecord getPersonRecordInstance() {
        if (personRecord == null) {
            personRecord = new PersonRecord();
        }

        return personRecord;
    }

    private static EntityRecord getEntityRecordInstance() {
        if (entityRecord == null) {
            entityRecord = new EntityRecord();
        }

        return entityRecord;
    }

    private static VesselRecord getVesselRecordInstance() {
        if (vesselRecord == null) {
            vesselRecord = new VesselRecord();
        }

        return vesselRecord;
    }

    public static PersonRecord getPersonRecordIfAny(Record record) {
        if (record.getType() == RecordType.PERSON) {
            return (PersonRecord) record;
        } else {
            return getPersonRecordInstance();
        }
    }

    public static EntityRecord getEntityRecordIfAny(Record record) {
        if (record.getType() == RecordType.ENTITY) {
            return (EntityRecord) record;
        } else {
            return getEntityRecordInstance();
        }
    }

    public static VesselRecord getVesselRecordIfAny(Record record) {
        if (record.getType() == RecordType.VESSEL) {
            return (VesselRecord) record;
        } else {
            return getVesselRecordInstance();
        }
    }
}
