package com.bnpp.regliss.entity.referential;

import java.util.stream.Stream;

public enum FileExportMode {
    FULL("full"),
    ADD_MOD("incremental"),
    ADD("incrementalAdd"),
    MOD("incrementalModify"),
    SUPP("incrementalDelete"),
    SYNTHESIS_OF_ADD("incrementalSynthesisOfAdd"),
    SYNTHESIS_OF_MOD("incrementalSynthesisOfModify"),
    SYNTHESIS_OF_SUPP("incrementalSynthesisOfDelete");

    private final String exportTypeAttribute;

    private FileExportMode(String exportTypeAttribute) { this.exportTypeAttribute = exportTypeAttribute; }

    public String toExportTypeAttribute() { return exportTypeAttribute; }

    public FileExportMode getActualExportMode(boolean hasPreviousVersion) {
        if (!hasPreviousVersion
                && Stream.of(FileExportMode.SYNTHESIS_OF_SUPP, FileExportMode.SYNTHESIS_OF_MOD,
                        FileExportMode.SUPP, FileExportMode.MOD).noneMatch(t -> t.equals(this))) {// TODO verify this method
            return FileExportMode.FULL;
        } else {
            return this;
        }
    }
}
