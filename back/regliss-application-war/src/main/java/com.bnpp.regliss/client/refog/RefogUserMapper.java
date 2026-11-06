package com.bnpp.regliss.client.refog;

// TODO: imports (lines 3-10) were folded in the IDE; reconstructed from usage - verify packages.
import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.Country;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.CountryRepository;
import org.apache.commons.lang3.StringUtils;      // TODO verify: could be org.springframework.util.StringUtils
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@ReglissWebProfile
public class RefogUserMapper {

    @Autowired
    private CountryRepository countryRepo;

    public User toUserEntity(RefogUser refogUser) {
        User newUser = new User();
        newUser.setFirstName(refogUser.getFirstName());
        newUser.setLastName(refogUser.getLastName());
        newUser.setUsername(refogUser.getUsername());
        newUser.setBusinessLine(refogUser.getBusinessLine());
        newUser.setCountry(getCountryFromCountryName(refogUser.getCountryName()));
        newUser.setEmail(refogUser.getEmail());
        newUser.setEntity(refogUser.getEntity());
        newUser.setPole(refogUser.getPole());
        return newUser;
    }

    private Country getCountryFromCountryName(String countryName) {
        if(!StringUtils.isEmpty(countryName)){
            return
                    countryRepo.findByLabelIgnoreCaseAndV3OnlyFalse(countryName)
                            .orElse(countryRepo.findByRefogLabelMapping(countryName)
                                    .orElseThrow(()->new ReglissException(ReglissException.ErrorCode.CREATE_USER_REFOG_COUNTRY_NOT_IN_REGLISS)));

        }
        return null;
    }
}
