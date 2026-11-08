package com.bnpp.regliss.facade.mapper;

// [MISSING FROM SCREENSHOTS: lines 3-9 of the original file were not visible in the photos]

@Service
public class UserGuideMapper {

    public UserGuideDto toDto(UserGuideFile userGuideFile) {
        UserGuideDto dto = new UserGuideDto();
        dto.id = userGuideFile.getId();
        dto.fileName = userGuideFile.getFileName();
        dto.userName = userGuideFile.getUserName();
        return dto;
    }
}
