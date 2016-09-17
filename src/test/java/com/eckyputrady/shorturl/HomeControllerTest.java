package com.eckyputrady.shorturl;

import com.jayway.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.Matchers.equalTo;

/**
 * Created by ecky on 08/06/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@IntegrationTest("server.port:0")
@WebAppConfiguration
public class HomeControllerTest {

    // Will contain the random free port number
    @Value("${local.server.port}")
    private int port;

    @Before
    public void setup() {
        RestAssured.port = port;
    }

    @Test
    public void testHome() {
        RestAssured
                .get("/")
                .then()
                    .body(equalTo("Hello!"));
    }
}