package com.bnpp.regliss.six.exporter.generated;

import javax.xml.bind.annotation.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "records")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "recordsType", propOrder = {"recordObj"})
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RecordsType {

    @XmlElement(name = "record", required = true)
    private List<RecordType> recordObj = new ArrayList<>();

    @XmlTransient
    public static class RecordsTypeBuilder { /* generated - empty */ }
}
