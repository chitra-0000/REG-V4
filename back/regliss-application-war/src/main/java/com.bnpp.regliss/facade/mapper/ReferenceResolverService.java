package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.facade.ReferenceFacade;
import com.bnpp.regliss.facade.dto.reference.ReferentialType;
import com.bnpp.regliss.profile.ReglissExternalAPIProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ReglissExternalAPIProfile
public class ReferenceResolverService {

    @Autowired
    private ReferenceFacade referenceFacade;

    public String resolveReferential(ReferentialType refType, Long refKey) {
        return resolveReferential(refType,refKey.toString());
    }

    public String resolveReferential(ReferentialType refType, Enum<?> e) {
        return resolveReferential(refType, String.valueOf(e));
    }

    private String resolveReferential(ReferentialType refType, String refKey) {
        Map<String, String> ref = getReferential(refType);
        if(!ref.containsKey(refKey)) {
            //should invalidate cache
            referenceFacade.clearListAndRecordReferentialMap();
            ref = getReferential(refType);
            if(!ref.containsKey(refKey)) {
                return "N/A";
            }

        }
        return ref.get(refKey);
    }

    private Map<String, String> getReferential(ReferentialType refType) {
        Map<ReferentialType, Map<String, String>> referential = referenceFacade.retrieveListAndRecordReferentialMap();
        return referential.get(refType);
    }
}
