package com.chef.william.service.discovery.provider;

import java.util.List;

public interface CityDiscoveryProvider {
    List<CityDiscoveryCandidate> discoverSupermarkets(String city);
}
