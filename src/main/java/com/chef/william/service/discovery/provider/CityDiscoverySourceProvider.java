package com.chef.william.service.discovery.provider;

import java.util.List;

public interface CityDiscoverySourceProvider {
    List<CityDiscoveryCandidate> discoverSupermarkets(String city);

    String sourceName();
}
