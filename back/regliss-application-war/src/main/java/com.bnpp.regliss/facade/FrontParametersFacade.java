package com.bnpp.regliss.facade;

// [MISSING FROM SCREENSHOTS: lines 3-7 of the original file were not visible in the photos]

@Facade
@ReglissWebProfile
public class FrontParametersFacade {

    @Value("${file.download.days.before.expiration}")
    private int nrOfDaysDownloadRight;

    public FrontParametersDto getParameters() {
        FrontParametersDto paramsDto = new FrontParametersDto();
        paramsDto.nrOfDaysDownloadRight = nrOfDaysDownloadRight;
        return paramsDto;
    }

}

