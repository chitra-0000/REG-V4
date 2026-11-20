package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.annotation.ChildType;
import com.bnpp.regliss.entity.MappingColumn;
import com.bnpp.regliss.entity.MappingFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class RecordFieldIndexes {
    private final EntityFieldIndexes allFieldIndexes;
    private final Map<ChildType, List<EntityFieldIndexes>> childMappings = new HashMap<>(); // NOSONAR for performance

    public RecordFieldIndexes(MappingFile mapping) {
        allFieldIndexes = new EntityFieldIndexes(mapping.getColumns());
        Stream.of(ChildType.values()).forEach(childType -> childMappings.put(childType, new ArrayList<>()));
        for (ChildType childType : ChildType.values()) {
            for (List<MappingColumn> childColumns : mapping.getChildrenColumns(childType)) {
                List<EntityFieldIndexes> list = childMappings.get(childType);
                list.add(new EntityFieldIndexes(childColumns));
            }
        }

    }
    public EntityFieldIndexes forRecord() { return allFieldIndexes; }
    public List<EntityFieldIndexes> forChild(ChildType childType) { return childMappings.get(childType); }
}

