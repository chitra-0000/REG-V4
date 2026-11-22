package com.bnpp.regliss.repository;

import java.util.Map;

public interface ProgramRepositoryCustom {
    void relinkRecordsTo(Map<Long, Long> newToCorrectProgramIds);
}

