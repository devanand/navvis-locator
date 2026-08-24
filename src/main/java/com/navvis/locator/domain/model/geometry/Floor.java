package com.navvis.locator.domain.model.geometry;

public record Floor(
        String name,
        Polygon2D outline,
        HeightRange height
) {

    /**
     * Determines whether a 3D point is located on this floor.
     *
     * A point is on this floor if and only if:
     * 1. Its z coordinate falls within this floor's height range
     * 2. Its (x, y) position falls within this floor's 2D outline
     *
     * Both conditions must be true — height alone is not enough,
     * since floors can have different outlines (e.g. a setback floor
     * that is smaller than the building envelope).
     */
    public boolean contains(double x, double y, double z) {
        return height.contains(z) && outline.contains(new Point2D(x, y));
    }
}