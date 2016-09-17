package com.eckyputrady.shorturl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class HomeController {

    @Value("${USER_AGENT:none}")
    private String userAgent;
    
    @RequestMapping("/")
    public String index() {
        return "Hello!";
    }

    @RequestMapping("/useragent")
    public String getUserAgent() {
        return userAgent;
    }
}
