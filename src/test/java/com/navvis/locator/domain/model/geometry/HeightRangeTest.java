package com.navvis.locator.domain.model.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeightRangeTest {

    private final HeightRange range = new HeightRange(0.0, 10.0);

    @Test
    @DisplayName("z inside the range returns true")
    void inside() {
        assertTrue(range.contains(5.0));
    }

    @Test
    @DisplayName("z at the lower boundary is inclusive")
    void lowerBoundary() {
        assertTrue(range.contains(0.0));
    }

    @Test
    @DisplayName("z at the upper boundary is inclusive")
    void upperBoundary() {
        assertTrue(range.contains(10.0));
    }

    @Test
    @DisplayName("z below the range returns false")
    void below() {
        assertFalse(range.contains(-0.001));
    }

    @Test
    @DisplayName("z above the range returns false")
    void above() {
        assertFalse(range.contains(10.001));
    }
}