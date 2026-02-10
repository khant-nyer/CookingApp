package com.chef.william.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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

        String normalized = value.trim();

        if (normalized.equalsIgnoreCase("STK")) {
            return PIECE;
        }

        return Arrays.stream(values())
                .filter(unit -> unit.abbreviation.equalsIgnoreCase(normalized)
                        || unit.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }

    @JsonCreator
    public static Unit fromJson(String value) {
        Unit unit = fromAbbreviation(value);
        if (unit == null) {
            throw new IllegalArgumentException("Unknown unit: " + value);
        }
        return unit;
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }

}
