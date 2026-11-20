package com.bnpp.regliss.service.pdf;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.UserGuideFile;
import com.bnpp.regliss.helper.FileUtils;
import com.bnpp.regliss.utils.BlobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.bnpp.regliss.repository.UserGuideFileRepository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserGuideService {

    @Autowired
    private BlobService blobService;

    @Autowired
    private UserGuideFileRepository repo;

//    @Autowired
//    private BlobService blobService;

    public void persistUserGuideService(MultipartFile multipartFile, String user){

        UserGuideFile userGuideFile;
        List<UserGuideFile> list = repo.findAll();
        if( list==null || list.size() == 0){
            userGuideFile = new UserGuideFile();
            repo.save(userGuideFile);
        }else{
            userGuideFile = list.get(0);
        }
        userGuideFile.setFileName(multipartFile.getOriginalFilename()) ;
        userGuideFile.setUploadDate(LocalDateTime.now());
        userGuideFile.setUserName(user);
        tryToStoreBlob(multipartFile,userGuideFile);
    }

    public Optional<UserGuideFile> getUserGuideFile(){
        List<UserGuideFile> list = repo.findAll();
        return list.size() == 0 ? Optional.empty() : Optional.of(list.get(0));
    }

//    private void uploadFileContent(UserGuideFile userGuideFile, MultipartFile multipartFile){
//        log.debug("start uploading userGuide file {} ", multipartFile.getOriginalFilename());
//        File zipArchive = fileUtils.createZippedArchive(fileUtils.getInputFile(multipartFile),multipartFile.getOriginalFilename());
//        tryToStoreBlob(zipArchive, userGuideFile);
//
//    }

    private void tryToStoreBlob(MultipartFile inputFile, UserGuideFile userGuideFile) {
        try {
            userGuideFile.setFileContent(blobService.createBlob(inputFile.getSize(), inputFile.getInputStream()));
        } catch (ReglissException | IOException e) {
            log.warn("Could not store userGuide file {} - {}", inputFile.getOriginalFilename(), e.getMessage());
        }
    }
}

