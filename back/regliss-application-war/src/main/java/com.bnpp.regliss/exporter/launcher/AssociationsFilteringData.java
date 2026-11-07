package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.repository.AssociationExternalReferenceCanonicFactory;
import com.bnpp.regliss.vo.AssociationExternalReference;
import com.bnpp.regliss.vo.RecordAssociationVO;
import com.bnpp.regliss.vo.RecordFilteringIdentifiers;
import com.bnpp.regliss.vo.RecordIdType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AssociationsFilteringData {

    private final Map<RecordFilteringIdentifiers, List<RecordAssociationVO>> recordsToTargetAssociatedRCAs;
    private final AssociationExternalReferenceCanonicFactory extRefFactory;
    private final Map<RecordIdType, List<AssociationExternalReference>> recordsToAllAssociations;

}
