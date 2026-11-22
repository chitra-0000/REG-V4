package com.bnpp.regliss.statistics;

// TODO: [GAP - imports are folded/collapsed in the IDE ("import ...;"), actual import list not visible in screenshot]

@Getter
@Getter
public class DjLogLivrasionStatistics {

    private int totalRecords;
    private int totalEntities;
    private int totalPersons;
    private int addRecords;
    private int addEntities;
    private int addPersons;
    private int modRecords;
    private int modEntities;
    private int modPersons;
    private int supRecords;
    private int supEntities;
    private int supPersons;
    private String uploadDate;


    public DjLogLivrasionStatistics() {
    }

    public DjLogLivrasionStatistics(int totalRecords, int addRecords, int modRecords, int supRecords) {
        this.totalRecords = totalRecords;

        this.addRecords = addRecords;

        this.modRecords = modRecords;

        this.supRecords = supRecords;
    }
}
