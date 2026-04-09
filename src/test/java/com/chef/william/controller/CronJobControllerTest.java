package com.chef.william.controller;

import com.chef.william.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CronJobControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CronJobController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void pingShouldReturnPingWhenSecretIsValid() throws Exception {
        mockMvc.perform(get("/api/cronjob/ping")
                        .header("x-cron-secret", "a3f8b2c9d4e7f1a6b8c3d5e9f2a4b7c1d6e8f3a5b9c2d4e7f1a3b6c8d5e9f2a4"))
                .andExpect(status().isOk())
                .andExpect(content().string("ping"));
    }

    @Test
    void pingShouldReturnUnauthorizedWhenSecretIsInvalid() throws Exception {
        mockMvc.perform(get("/api/cronjob/ping")
                        .header("x-cron-secret", "invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid cron secret"));
    }
}
