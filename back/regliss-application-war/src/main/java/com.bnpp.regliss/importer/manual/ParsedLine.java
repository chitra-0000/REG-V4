package com.bnpp.regliss.importer.manual;

import java.util.List;

public class ParsedLine {
    public final int lineNumber;
    public final List<String> cells;
    public ParsedLine(int lineNumber, List<String> cells) {
        this.lineNumber = lineNumber;
        this.cells = cells;
    }
}

