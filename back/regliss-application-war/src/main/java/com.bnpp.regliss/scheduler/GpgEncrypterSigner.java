package com.bnpp.regliss.scheduler;

import java.io.*;
import java.util.*;

import org.apache.commons.io.*;
import org.apache.commons.lang3.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import com.bnpp.regliss.*;
import com.bnpp.regliss.profile.*;

import lombok.extern.slf4j.*;

@Service
@ReglissPublisherProfile
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED) // not to break the received transaction
public class GpgEncrypterSigner {

    @Value("${gpg.command.base}")
    private String gpgBaseCommand;
    @Value("${regliss.key.userid}")
    private String reglissSignKeyUserId;
    @Value("${gpg.logFile}")
    private String logFile;
    private static final String GPG_FAILED = "GPG failed: ";

    public GpgEncrypterSigner() {
        // Do nothing
    }

    public File encryptFileAndSign(String filePath, String receiverPublicKeyRef) {
        String commandWithArgs = buildGPGCommand(receiverPublicKeyRef, filePath);
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", commandWithArgs);//for windows ("CMD", "/c", commandWithArgs)
        pb.redirectErrorStream(true);
        try {
            log.info("Command run: " + pb.command().toString());
            handleProcessExecution(pb);

        } catch (IOException e) {
            log.error(GPG_FAILED, e);
            throw new ReglissException(e);
        }  catch (InterruptedException e) {
            log.error(GPG_FAILED, e);
            Thread.currentThread().interrupt();
            throw new ReglissException(e);
        }
        return new File(filePath + ProcessUtil.PGP);
    }

    public void removingOriginalFileAndEncrytptedFile(String filePath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", "rm -rf "+filePath);
        pb.redirectErrorStream(true);
        try {
            log.info("Command run: " + pb.command().toString());
            handleProcessExecutionForDeletion(pb);
        } catch (IOException e) {
            log.error(GPG_FAILED, e);
            throw new ReglissException(e);
        }  catch (InterruptedException e) {
            log.error(GPG_FAILED, e);
            Thread.currentThread().interrupt();
            throw new ReglissException(e);
        }
    }



    private String buildGPGCommand(String receiverPublicKeyRef, String filePath) {
        String encryptedSignedFilePath = filePath + ProcessUtil.PGP;
        return gpgBaseCommand+" --batch --yes --always-trust -v --cipher aes256 --log-file "+logFile+
                " --local-user "+reglissSignKeyUserId+
                " -r "+receiverPublicKeyRef+
                " -o "+encryptedSignedFilePath+
                " -s -e "+filePath;
    }

    protected void handleProcessExecution(ProcessBuilder pb) throws IOException, InterruptedException {
        Process process = pb.start();
        int exitCode = process.waitFor();
        log.info("GPG exited with exit code : " + exitCode);
        List<String> redLines = IOUtils.readLines(process.getInputStream());
        String response = String.join("\n", redLines);
        log.info("GPG response: \n{}", response);

        if (exitCode != 0) {
            throw new ReglissException(String.format("GPG exited with code %s, response: %s", exitCode, response)) ;
        }
    }

    protected void handleProcessExecutionForDeletion(ProcessBuilder pb) throws IOException, InterruptedException {
        Process process = pb.start();
        int exitCode = process.waitFor();
        log.info("Failed to delete the files : " + exitCode);
        List<String> redLines = IOUtils.readLines(process.getInputStream());
        String response = String.join("\n", redLines);
        log.info("Deleted the original file: \n{}", response);

        if (exitCode != 0) {
            throw new ReglissException(String.format("Deleted the file exited with code %s, response: %s", exitCode, response)) ;
        }
    }

}

