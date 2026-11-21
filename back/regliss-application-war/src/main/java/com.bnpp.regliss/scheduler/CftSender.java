package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.profile.ReglissPublisherProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@ReglissPublisherProfile
@Slf4j
@Transactional(propagation=Propagation.NOT_SUPPORTED) // not to break the received transaction
public class CftSender {

    private static final Logger log = LoggerFactory.getLogger(CftSender.class);

    @Value("${cft.max.wait.seconds}")
    private long cftMaxWaitSeconds;

    @Value("${cft.process.log.folder}")
    private File processLogFolder;

    @Value("${cft.send.command.base}") // should be CFTUTIL on UNIX, or a dummy .bat/.sh on Windows/DEV
    private String cftSendCommand;

    @Value("${cft.push.folder.base}")
    private File pushFolderBase;

    @Autowired
    private FileRepository fileRepo;

    @PostConstruct
    public void checkFolder() {
        if (!pushFolderBase.isDirectory()) {
            throw new IllegalArgumentException("Should be an accesible directory: " + pushFolderBase);
        }
    }

    public File getDestinationFolder(String specificFolderName) {
        File folder = fileRepo.makeFolder(pushFolderBase, specificFolderName);
        if (!folder.isDirectory()) {
            log.debug("Creating folder: {}" , folder.getAbsolutePath());
            boolean ok = folder.mkdirs();
            if (ok) {
                log.debug("Created ok");
                folder.setExecutable(true, false);
            } else {
                throw new RuntimeException("Could not create folder for subscriber: " + folder);
            }
        }
        return folder;
    }

    public String sendFile(String part, String idf, String fname, String parm) {
        waitForNoCFTRunning();

        String command = cftSendCommand +
                " SEND PART=" + part +
                ", IDF=" + idf +
                ", FNAME=" + fname +
                ", PARM='" + parm+"'";

        waitUntilNext10secMoment();
        log.info("execute cftSendCommand:{}",command);
        String logTimeId = executeCommand(command, cftMaxWaitSeconds);
        return logTimeId;
    }

    private void waitUntilNext10secMoment() {
        try {
            long deltaToWait = 10_000 - System.currentTimeMillis() % 10_000;
            log.debug("Waiting until next timestamp multiple of 10 seconds to have different timestamp IDs. Millis to wait: {}", deltaToWait);
            Thread.sleep(deltaToWait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void waitForNoCFTRunning() {
        boolean allStopped = ProcessUtil.waitForNoProcessesNamed("CFTUTIL", cftMaxWaitSeconds);
        if (allStopped) {
            log.debug("No CFT process is currently running");
        } else {
            log.warn("Maximum wait time reached. A CFTUTIL process is still running. Proceeding anyway...");
        }
    }

    private String executeCommand(String commandWithArgs, long maxWaitSeconds) {
        String logFileName = "exec-" + new SimpleDateFormat("yyyy-MM-dd@HH-mm-ss").format(new Date()) + ".log";
        File logFile = new File(processLogFolder, logFileName);

        log.debug("Starting command {}", commandWithArgs);
        log.debug("Logging into file: {}", logFile.getAbsolutePath());

        int errorCode = ProcessUtil.getExitCode(commandWithArgs, maxWaitSeconds, logFile, line -> log.debug("> {}" , line));

        log.debug("CFT invocation completed OK with errorCode {}", errorCode);

        try (Reader stdoutFileReader = new FileReader(logFile)) {
            return extractTimestampID(IOUtils.toString(stdoutFileReader));
        } catch (IOException e) {
            throw new RuntimeException("Could not read stdout log file", e);
        }
    }

    // Communication file row number used: 00000074 on 20170817 Time 10190992 results in ---> 1710191
    private static final Pattern TIME_PART_PATTERN = Pattern.compile(" on \\d{6}(\\d{2}) Time (\\d{4})(\\d{4})");

    public static String extractTimestampID(String cftStdout) {
        try {
            Matcher matcher = TIME_PART_PATTERN.matcher(cftStdout);
            if (!matcher.find()) {
                throw new IllegalArgumentException("CFTUTIL call failed: output did not contain the pattern "
                        + TIME_PART_PATTERN.pattern() + ": \n" + cftStdout);
            }
            String underMinutesStr = matcher.group(3);
            int underMinutesRounded = (int) Math.round(Integer.parseInt(underMinutesStr) / 1000d);
            String timestampID = matcher.group(1) + matcher.group(2) + underMinutesRounded;

            log.trace("Extracted day+time: {} --> Computed timestamp ID: {}",matcher.group(), timestampID);
            return timestampID;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public File getPushFolderBase() {
        return pushFolderBase;
    }

    public void setPushFolderBase(File pushFolderBase) {
        this.pushFolderBase = pushFolderBase;
    }
}

