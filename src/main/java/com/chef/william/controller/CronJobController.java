package com.chef.william.controller;

import com.chef.william.exception.UnauthorizedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cronjob")
public class CronJobController {

    private static final String CRON_SECRET_HEADER = "x-cron-secret";
    private static final String CRON_SECRET_VALUE = "a3f8b2c9d4e7f1a6b8c3d5e9f2a4b7c1d6e8f3a5b9c2d4e7f1a3b6c8d5e9f2a4";

    @GetMapping("/ping")
    public String ping(@RequestHeader(name = CRON_SECRET_HEADER, required = false) String cronSecret) {
        if (!CRON_SECRET_VALUE.equals(cronSecret)) {
            throw new UnauthorizedException("Invalid cron secret");
        }
        return "ping";
    }
}
