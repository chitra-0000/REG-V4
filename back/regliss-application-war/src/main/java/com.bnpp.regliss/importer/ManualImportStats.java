package com.bnpp.regliss.importer;

public class ManualImportStats {
    private int totalCreated;
    private int totalUpdated;
    private int totalDeleted;

    public void addCreated() { totalCreated ++; }
    public void addUpdated() { totalUpdated ++; }

    public void setTotalDeleted(int totalDeleted) { this.totalDeleted = totalDeleted; }

    @Override
    public String toString() {
        return "Created=" + totalCreated +
                ", Updated=" + totalUpdated +
                ", Deleted=" + totalDeleted;
    }
}

