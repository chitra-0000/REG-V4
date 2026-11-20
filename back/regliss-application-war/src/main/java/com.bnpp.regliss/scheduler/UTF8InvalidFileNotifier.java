package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.service.EmailService;
import com.bnpp.regliss.vo.ListIdAndVersionId;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UTF8InvalidFileNotifier {

    private static final String INVALID_UTF8 = "invalid UTF-8 code";

    @Value("${file.isutf8.command.base}") // should be isutf8 on UNIX, or a dummy .bat/.sh on Windows/DEV
    private String isUTF8SendCommand;

    @Autowired
    private EmailService emailService;

    private String quote(String value) {
        return value.contains(" ")
                ? '"' + value + '"'
                : value;
    }

    String checkFile(Path filePath) {
        String line = Strings.EMPTY;

        String[] specialChars = new String[]{"&", "&&", "|", "||"};
        String[] replacementList = new String[]{"X", "Y", "Z", "W",};
        Path normalized = filePath.normalize();
        ProcessBuilder pb = new ProcessBuilder(
                quote(StringUtils.replaceEach(isUTF8SendCommand, specialChars, replacementList)),
                quote(normalized.toString()));
        pb.redirectErrorStream(true);

        // Run this on Windows, cmd, /c = terminate after this run
        try {
            log.info("Command run: " + pb.command().toString());
            Process process = pb.start();
            int exitCode = process.waitFor();
            line = handleProcessExecution(process, exitCode, line);

        } catch (IOException e) {
            log.error("ISUTF8 failed: ", e);
            return line;
        } catch (InterruptedException e) {
            log.error("ISUTF8 failed: ", e);
            Thread.currentThread().interrupt();
            return line;
        }
        return line;
    }

    private String handleProcessExecution(Process process, int exitCode, String line) throws IOException {
        log.info("ISUTF8 exited with exit code : " + exitCode);
        List<String> redLines = IOUtils.readLines(process.getInputStream());
        if (exitCode == 1) {
            line = redLines.stream().findFirst().orElse(null);
        }
        log.info("ISUTF8 response: \n{}", String.join("\n", redLines));

        return line;
    }

    public void sendMailIfNotValidUTF8File(Path filePath, String isUTF8Response, ListIdAndVersionId listIdAndVersionId) {
        emailService.sendEmailImportError(filePath.toString() + " " + isUTF8Response, listIdAndVersionId);
    }

    public Optional<String> getUtf8ErrorMessage(Path filePath) {
        String isUTF8Response = checkFile(filePath);
        if (StringUtils.isNotBlank(isUTF8Response) && isUTF8Response.contains(INVALID_UTF8)) {
            return Optional.of(isUTF8Response);
        }
        return Optional.empty();
    }

}

