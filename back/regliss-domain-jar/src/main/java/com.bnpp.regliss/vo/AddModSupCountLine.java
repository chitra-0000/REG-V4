package com.bnpp.regliss.vo;

import java.util.List;

import lombok.Data;

@Data
public class AddModSupCountLine {

    private final AddModSupCountType elementType;
    private final int addCount;
    private final int modCount;
    private final int supCount;
    private final int totalCount;

    public AddModSupCountLine(AddModSupCountType elementType, int addCount, int modCount, int supCount, int totalCount) {
        this.elementType = elementType;
        this.addCount = addCount;
        this.modCount = modCount;
        this.supCount = supCount;
        this.totalCount = totalCount;
    }

    public AddModSupCountLine(List<Object[]> columns) {
        AddModSupCountType elemType = null;
        int columnAddCount = 0;
        int columnModCount = 0;
        int columnSupCount = 0;
        for (Object[] column : columns) {
            elemType = AddModSupCountType.valueOf((String) column[0]);
            AddModSupAction action = AddModSupAction.fromStringAction((String) column[1]);
            int count = ((Number) column[2]).intValue();

            switch (action) {
                case ADD:
                    columnAddCount = count;
                    break;
                case MOD:
                    columnModCount = count;
                    break;
                case SUP:
                    columnSupCount = count;
                    break;
                default:
                    throw new IllegalArgumentException("Not a valid action: " + action);
            }
        }

        this.elementType = elemType;
        this.addCount = columnAddCount;
        this.modCount = columnModCount;
        this.supCount = columnSupCount;
        this.totalCount = columnAddCount + columnModCount + columnSupCount;
    }

    public static AddModSupCountLine createEmptyLine(AddModSupCountType elementType) {
        return new AddModSupCountLine(elementType, 0, 0, 0, 0);
    }

    public static AddModSupCountLine createTotalsLine(List<AddModSupCountLine> list) {
        int addCount = list.stream().mapToInt(AddModSupCountLine::getAddCount).sum();
        int modCount = list.stream().mapToInt(AddModSupCountLine::getModCount).sum();
        int supCount = list.stream().mapToInt(AddModSupCountLine::getSupCount).sum();
        int totalCount = list.stream().mapToInt(AddModSupCountLine::getTotalCount).sum();
        return new AddModSupCountLine(AddModSupCountType.TOTAL, addCount, modCount, supCount, totalCount);
    }

    public boolean isOfType(AddModSupCountType elementType) {
        return elementType == this.elementType;
    }

    public String getElementTypeName() {
        return elementType.label;
    }
}
