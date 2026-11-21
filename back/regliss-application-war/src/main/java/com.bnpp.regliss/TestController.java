package com.bnpp.regliss;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

@RestController
public class TestController {

    @Autowired
    DataSource dataSource;
    @Value("${spring.datasource.url}")
    private String value;
    @Value("${min.available.free.spaceMB}")
    private String aaa;

    @GetMapping("hello")
    public String hello() {
        return "HJello " + value + "      " + dataSource + " " + aaa;
    }
}

