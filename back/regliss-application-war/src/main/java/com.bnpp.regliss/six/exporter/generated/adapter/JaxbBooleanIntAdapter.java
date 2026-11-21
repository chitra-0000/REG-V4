package com.bnpp.regliss.six.exporter.generated.adapter;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class JaxbBooleanIntAdapter extends XmlAdapter<Integer, JAXBElement<Boolean>> {

    @Override
    public Integer marshal(JAXBElement<Boolean> v) throws Exception {
        if(v.getValue()) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public JAXBElement<Boolean> unmarshal(Integer v) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
