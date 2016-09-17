package com.eckyputrady.shorturl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@Slf4j
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

    @RequestMapping("/github/webhook")
    public String githubWebhook(
            @RequestHeader(value = "X-GitHub-Event") String event,
            @RequestHeader(value = "X-GitHub-Delivery") String id,
            @RequestBody String payload
    ) {
        log.info("ID={}, Event={}, Payload={}", id, event, payload);
        return "";
    }
}
