package com.chef.william.dto.discovery;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SupermarketDTO {
    String name;
    String city;
    String country;
    String address;
    Double latitude;
    Double longitude;
    String source;
}
