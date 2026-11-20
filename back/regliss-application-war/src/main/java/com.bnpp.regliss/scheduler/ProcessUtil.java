package com.bnpp.regliss.scheduler;

import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.xml.ws.Holder;

import com.bnpp.regliss.ReglissException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessUtil {

    public static final String PGP = ".pgp";

    public static boolean waitForNoProcessesNamed(String processNamePart, long maxWaitSeconds) {
        log.debug("Searching for running instances of processes containing '{}'...", processNamePart);
        long t0 = System.currentTimeMillis();
        while (System.currentTimeMillis() - t0 < maxWaitSeconds * 1000) {
            if (!isProcessRunning(processNamePart)) {
                return true;
            }
            try{
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ReglissException(e);
            }
        }
        return false;
    }

    private static boolean isProcessRunning(String processName) {
        try {
            if (System.getProperty("os.name", "").startsWith("Windows")) {
                Process process = new ProcessBuilder("cmd.exe ", "/C", "tasklist.exe /FI \"IMAGENAME eq "+processName+"\" | find /I /N \""+processName+"\"")
                        .redirectErrorStream(true).start();
                return process.waitFor() == 0;
            } else {
                Process process = new ProcessBuilder("sh", "-c", "ps -ef | grep \""+processName+"\" | grep -v grep | wc -l")
                        .redirectErrorStream(true).start();
                process.waitFor();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    List<String> lines = reader.lines().collect(toList());
                    log.debug("Number of lines in buffered reader {} ", lines.size());
                    return Integer.parseInt(lines.get(lines.size()-1).trim()) > 0  ;
                }
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReglissException(e, "Could not count the active processes");
        }
    }

    public static int getExitCode(String commandWithArgs, long maxWaitSeconds, File logFile, Consumer<String> outputLineConsumer) {
        Process process;
        try {
            process = new ProcessBuilder(commandWithArgs.split("\\s+"))
                    .redirectOutput(logFile)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new ReglissException(e, "Could not launch command: " + commandWithArgs);
        }

        long t0 = System.currentTimeMillis();

        Holder<Integer> lastLine = new Holder<>(0);

        while (System.currentTimeMillis() - t0 < maxWaitSeconds * 1000L) {

            printDeltaLogLines(logFile, lastLine, outputLineConsumer);
            if (!process.isAlive()) {
                return process.exitValue();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ReglissException(e);
            }
        }

        throw new ReglissException("The process did not terminate in the allowed timeframe (" + maxWaitSeconds + ")");
    }

    private static void printDeltaLogLines(File logFile, Holder<Integer> lastLine, Consumer<String> outputLineConsumer) {
        try (Stream<String> logLinesStream = Files.lines(logFile.toPath())) {
            List<String> logLines = logLinesStream.skip(lastLine.value).collect(toList());

            for (String logLine : logLines) {
                outputLineConsumer.accept(logLine);
            }
            lastLine.value += logLines.size();
        } catch (IOException logException) {
            log.info("Could not read the log file");
            log.debug(logException.getMessage(), logException);
        }
    }

    private ProcessUtil(){}

}

