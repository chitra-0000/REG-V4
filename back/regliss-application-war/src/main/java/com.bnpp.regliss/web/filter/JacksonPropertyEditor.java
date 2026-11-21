package com.bnpp.regliss.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.beans.PropertyEditorSupport;

public class JacksonPropertyEditor extends PropertyEditorSupport {

    private final Class<?> clazz;

    public JacksonPropertyEditor(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * @throws IllegalArgumentException
     */
    @Override
    public void setAsText(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            setValue(mapper.readValue(json, clazz));
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON >>"+json+"<< cannot be parsed to an instance of " + clazz.getSimpleName(), e);
        }
    }
}

