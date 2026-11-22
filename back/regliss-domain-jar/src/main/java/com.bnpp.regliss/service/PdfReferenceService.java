package com.bnpp.regliss.service;

import com.bnpp.regliss.entity.referential.record.Gender;
import com.bnpp.regliss.entity.referential.record.PEPOccupationCategory;
import com.bnpp.regliss.repository.GenderRepository;
import com.bnpp.regliss.repository.PEPOccupationCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PdfReferenceService {

    @Autowired
    private PEPOccupationCategoryRepository pepOccupationCategoryRepo;

    @Autowired
    private GenderRepository genderRepository;

    public PEPOccupationCategory getPepOccupationCategoryById(Long id) {
        return pepOccupationCategoryRepo.getExactlyOne(id);
    }

    // TODO: [GAP - lines 24-25 not captured in screenshots: code was folded/collapsed in editor]

    public Gender getGenderById(Long id) { return genderRepository.getExactlyOne(id); }
}
