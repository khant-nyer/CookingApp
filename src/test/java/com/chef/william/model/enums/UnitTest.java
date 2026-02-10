package com.chef.william.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnitTest {

    @Test
    void fromJsonShouldMapStkAliasToPiece() {
        assertEquals(Unit.PIECE, Unit.fromJson("STK"));
    }

    @Test
    void fromJsonShouldThrowForUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> Unit.fromJson("UNKNOWN_UNIT"));
    }
}
