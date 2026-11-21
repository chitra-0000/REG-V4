package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.entity.record.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Supplier;

public enum RecordType {

    PERSON(PersonRecord::new),
    ENTITY(EntityRecord::new),
    VESSEL(VesselRecord::new),
    OTHER(OtherRecord::new),
    AIRPORT(AirportRecord::new),
    CITY(CityRecord::new),
    COUNTRY(CountryRecord::new),
    SEAPORT(SeaportRecord::new);

    public final Supplier<? extends Record> constructor;

    RecordType(Supplier<? extends Record> constructor) { this.constructor = constructor; }

    public Record instantiate() {
        Record record = constructor.get();
        if (record == null) {
            throw new IllegalArgumentException("Supplier must not return null");
        }
        return record;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DiscriminatorValues{

        public static final String PERSON = "PERSON";
        public static final String ENTITY = "ENTITY";
        public static final String VESSEL = "VESSEL";
        public static final String OTHER = "OTHER";
        public static final String COUNTRY = "COUNTRY";
        public static final String CITY = "CITY";
        public static final String SEAPORT = "SEAPORT";
        public static final String AIRPORT = "AIRPORT";
    }
}
