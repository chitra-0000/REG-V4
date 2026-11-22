package com.bnpp.regliss.vo;

import java.util.List;

import com.bnpp.regliss.entity.User;

import lombok.Data;

@Data
public class UserWithAccessibleListsVO {
    private final User user;
    private final List<String> listNames;
}
