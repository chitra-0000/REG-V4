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
public class PgpEncrypterSigner {

    @Value("${pgp.command.base}") // should be /opt/pgp/bin/pgp on UNIX, or a dummy .bat/.sh on Windows/DEV
    private String pgpBaseCommand;
    @Value("${regliss.key.userid}")
    private String reglissSignKeyUserId;

    public File encryptFileAndSign(String filePath, String receiverPublicKeyRef) {
        String bash="bash";
        String bashValue="-c";
        String commandWithArgs = buildPGPCommand(receiverPublicKeyRef, filePath);
        ProcessBuilder pb = new ProcessBuilder(bash, bashValue , commandWithArgs); // NOSONAR: The path is build internally and parameter is sanitized above with path normaliz
        pb.redirectErrorStream(true);

        try {
            log.info("Command run: " + pb.command().toString());
            handleProcessExecution(pb);

        } catch (IOException e) {
            log.error("PGP failed: ", e);
            throw new ReglissException(e);
        } catch (InterruptedException e) {
            log.error("PGP failed: ", e);
            Thread.currentThread().interrupt();
            throw new ReglissException(e);
        }

        return new File(filePath + ProcessUtil.PGP);
    }

    private String buildPGPCommand(String receiverPublicKeyRef, String filePath) {
        String encryptedSignedFilePath = filePath + ProcessUtil.PGP;
        return  pgpBaseCommand +
                " -v --force --cipher aes256 --hash sha256 -es " + StringUtils.wrap(filePath, "'") +
                " -r " + receiverPublicKeyRef +
                " --signer "+reglissSignKeyUserId +
                " --output " + StringUtils.wrap(encryptedSignedFilePath, "'") +
                " --overwrite remove --input-cleanup wipe"
                ;
    }

    protected void handleProcessExecution(ProcessBuilder pb) throws IOException, InterruptedException {
        Process process = pb.start();
        int exitCode = process.waitFor();
        log.info("PGP exited with exit code : " + exitCode);
        List<String> redLines = IOUtils.readLines(process.getInputStream());
        String response = String.join("\n", redLines);
        log.info("PGP response: \n{}", response);

        if (exitCode != 0) {
            throw new ReglissException(String.format("PGP exited with code %s, response: %s", exitCode, response)) ;
        }
    }

}

