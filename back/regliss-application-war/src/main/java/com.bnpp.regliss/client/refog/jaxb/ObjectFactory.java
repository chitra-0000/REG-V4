package com.bnpp.regliss.client.refog.jaxb;

import javax.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {
    public ObjectFactory() {
        //intentionally left empty
    }
    public Parameters createParameters() { return new Parameters(); }
    public Fields createFields() { return new Fields(); }
    public RefogSearch createRefogSearch() { return new RefogSearch(); }
    public Value createValue() { return new Value(); }
    public Field createField() { return new Field(); }
    public InnerField createInnerField() { return new InnerField(); }
    public Data createData() { return new Data(); }
    public Info createInfo() { return new Info(); }
    public Parameter createParameter() { return new Parameter(); }
    public Error createError() { return new Error(); }
    public Status createStatus() { return new Status(); }
    public Errors createErrors() { return new Errors(); }
    public Values createValues() { return new Values(); }
    public RefogObject createRefogObject() { return new RefogObject(); }
}
