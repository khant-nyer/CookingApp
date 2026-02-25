package com.chef.william.controller;

import com.chef.william.dto.discovery.DiscoverSupermarketsRequest;
import com.chef.william.dto.discovery.DiscoverSupermarketsResponse;
import com.chef.william.service.discovery.DiscoverSupermarketsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/supermarkets")
@RequiredArgsConstructor
public class DiscoverSupermarketsController {

    private final DiscoverSupermarketsService discoverSupermarketsService;

    @PostMapping("/discover-supermarkets")
    public ResponseEntity<DiscoverSupermarketsResponse> discoverSupermarkets(
            @Valid @RequestBody DiscoverSupermarketsRequest request) {
        return ResponseEntity.ok(discoverSupermarketsService.discoverSupermarkets(request));
    }
}
