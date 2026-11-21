package com.bnpp.regliss.entity.record;

import java.util.List;

public interface RecordAwareChild {
    List<? extends Record> getRecords(); //NOSONAR
    void removeFromParent(Record parentToUnlink);
}
