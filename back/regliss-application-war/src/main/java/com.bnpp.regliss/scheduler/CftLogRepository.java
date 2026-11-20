package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.profile.ReglissPublisherProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

@Repository
@ReglissPublisherProfile
public class CftLogRepository {

    @Value("${cft.log.file}")
    private File cftLogFile;

    public Stream<String> getLines() {
        try {
            return Files.lines(cftLogFile.toPath());
        } catch (IOException e) {
            throw new ReglissException(e, "Could not access the CFT Logs");
        }
    }

}


