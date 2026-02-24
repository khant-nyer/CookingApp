package com.chef.william.service.discovery.verification;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CatalogVerificationResult {
    private boolean matched;
    private String matchSource;
    private double confidence;
    private String failureReason;

    public CatalogVerificationResult(boolean matched, String matchSource) {
        this(matched, matchSource, matched ? 0.7 : 0.0, matched ? "" : "NO_MATCH_ON_CRAWL");
    }

    public CatalogVerificationResult(boolean matched, String matchSource, double confidence, String failureReason) {
        this.matched = matched;
        this.matchSource = matchSource;
        this.confidence = confidence;
        this.failureReason = failureReason == null ? "" : failureReason;
    }

    public static CatalogVerificationResult matched(String source, double confidence) {
        return new CatalogVerificationResult(true, source, confidence, "");
    }

    public static CatalogVerificationResult noMatch(String source, String reason) {
        return new CatalogVerificationResult(false, source, 0.0, reason);
    }
}
