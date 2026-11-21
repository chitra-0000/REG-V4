package com.bnpp.regliss.web.filter;

import java.beans.PropertyEditorSupport;

public class EnumPropertyEditor extends PropertyEditorSupport {

    private final Class<? extends Enum<?>> enumClass;

    public EnumPropertyEditor(Class<? extends Enum<?>> enumClass) {
        this.enumClass = enumClass;
    }

    /**
     * @throws IllegalArgumentException
     */
    @Override
    public void setAsText(String text) {
        for (Enum<?> enumValue : enumClass.getEnumConstants()) {
            if (enumValue.name().equalsIgnoreCase(text)) {
                setValue(enumValue);
                return;
            }
        }
        throw new IllegalArgumentException("Enum " + enumClass + " does not contain any contant with name (caseinsensitive) = " + text);
    }
}

