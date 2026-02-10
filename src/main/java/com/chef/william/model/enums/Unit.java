package com.chef.william.model.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Unit {
    G("g"),
    KG("kg"),
    MG("mg"),
    MCG("mcg"),
    ML("ml"),
    L("l"),
    TSP("tsp"),
    TBSP("tbsp"),
    CUP("cup"),
    OZ("oz"),
    LB("lb"),
    PIECE("piece"),
    PINCH("pinch"),
    CLOVE("clove"),
    SLICE("slice");

    private final String abbreviation;

    Unit(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public static Unit fromAbbreviation(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Arrays.stream(values())
                .filter(unit -> unit.abbreviation.equalsIgnoreCase(value)
                        || unit.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }

}
