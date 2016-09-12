package com.loacg.controller;

import com.loacg.kayo.handlers.Directions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Project: kayosan
 * Author: Sendya <18x@loacg.com>
 * Time: 8/4/2016 11:33 PM
 */
@RestController
@RequestMapping("/test")
public class Test {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Directions bot;

    @RequestMapping("/query/{name}")
    public @ResponseBody Object queryData(@PathVariable("name") String name) {

        return "success";
    }

    @RequestMapping("/send/voice")
    public @ResponseBody Object queryData() throws IOException {

        return "success";
    }

}
