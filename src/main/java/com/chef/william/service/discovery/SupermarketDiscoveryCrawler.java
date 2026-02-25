package com.chef.william.service.discovery;

import java.util.List;

public interface SupermarketDiscoveryCrawler {
    List<DiscoveredSupermarket> discover(String city, String countryCode);
}
