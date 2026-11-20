package com.bnpp.regliss.service.external.de;

import com.bnpp.regliss.profile.ReglissExternalAPIProfile;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static jdk.nashorn.internal.objects.NativeMath.log;

@Component
@ReglissExternalAPIProfile
@Log4j2
public class ProgramsCacheController {

    @Autowired
    private ProgramsCacheController selfProxy;

    @Value("${min.available.free.ramMB:256}")
    private long memoryTresholdMB;

    @CacheEvict(value="versionProgramLabels",allEntries = true)
    public void clearVersionProgramLabelsCache() {
        //performed by spring
    }

    @Scheduled(fixedRate = 60000)
    private void clearVersionProgramCacheOnMemoryTreshold(){
        long availableMemory = getMemoryFreeUsableMemory();
        log.debug("available memory:{} bytes",availableMemory);
        if(availableMemory< memoryTresholdMB *1024*1024){
            log.debug("clearing versionProgramLabels cache");
            selfProxy.clearVersionProgramLabelsCache();
        }
    }

    private long getMemoryFreeUsableMemory() {
        return Runtime.getRuntime().maxMemory()-Runtime.getRuntime().totalMemory()+Runtime.getRuntime().freeMemory();
    }

}

