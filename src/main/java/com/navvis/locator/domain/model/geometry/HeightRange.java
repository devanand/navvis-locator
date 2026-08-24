package com.navvis.locator.domain.model.geometry;

public record HeightRange(double min, double max) {

    public boolean contains(double z) {
        return z >= min && z <= max;
    }
}