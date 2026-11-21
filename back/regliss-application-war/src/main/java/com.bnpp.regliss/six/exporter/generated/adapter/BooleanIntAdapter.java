package com.bnpp.regliss.six.exporter.generated.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class BooleanIntAdapter extends XmlAdapter<Integer, Boolean> {

    @Override
    public Boolean unmarshal(Integer v) throws Exception {
        return v.equals(1);
    }

    @Override
    public Integer marshal(Boolean v) throws Exception {
        if(v) {
            return 1;
        } else {
            return 0;
        }
    }

}
