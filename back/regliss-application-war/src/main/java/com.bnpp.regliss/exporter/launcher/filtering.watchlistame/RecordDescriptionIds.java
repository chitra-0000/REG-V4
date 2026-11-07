package com.bnpp.regliss.exporter.launcher.filtering.watchlistame;

import java.io.Serializable;
import java.util.stream.IntStream;

public class RecordDescriptionIds implements Serializable {
    private static final long serialVersionUID = 1L;
    private int[] categories;
    private int[] sanctionTypes;
    private int[] descriptions3;
    private int[] pepOccupationCategories;
    private int[] genders;

    public RecordDescriptionIds() {
    }

    public RecordDescriptionIds(int[] categories, int[] sanctionTypes, int[] descriptions3, int[] pepOccupationCategories,int[] genders) {
        this.categories = categories;
        this.sanctionTypes = sanctionTypes;
        this.descriptions3 = descriptions3;
        this.pepOccupationCategories = pepOccupationCategories;
        this.genders = genders;
    }

    public boolean containsCategory(long id) {
        if (categories == null) {
            return false;
        }
        return IntStream.of(categories).anyMatch(n -> n == id);
    }

    public boolean containsSanctionType(long id) {
        if (sanctionTypes == null) {
            return false;
        }
        return IntStream.of(sanctionTypes).anyMatch(n -> n == id);
    }
    public boolean containsDescription3(long id) {
        if (descriptions3 == null) {
            return false;
        }
        return IntStream.of(descriptions3).anyMatch(n -> n == id);
    }
    public boolean containsPepOccupationCategory(long id) {
        if (pepOccupationCategories == null) {
            return false;
        }
        return IntStream.of(pepOccupationCategories).anyMatch(n -> n == id);
    }
    public boolean containsGenders(long id) {
        if (genders == null) {
            return false;
        }
        return IntStream.of(genders).anyMatch(n -> n == id);
    }
}

