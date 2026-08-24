package com.navvis.locator.domain.model.geometry;

import java.util.List;
import java.util.Optional;

public record Building(
        String name,
        Polygon2D outline,
        HeightRange height,
        List<Floor> floors
) {

    /**
     * Determines whether a 3D point is located within this building.
     *
     * A point is inside the building if:
     * 1. Its z coordinate falls within the building's overall height range
     * 2. Its (x, y) position falls within the building's 2D outline
     */
    public boolean contains(double x, double y, double z) {
        return height.contains(z) && outline.contains(new Point2D(x, y));
    }

    /**
     * Finds which floor within this building the point is on.
     *
     * Returns empty if the point is inside the building envelope
     * but not within any floor's outline or height range —
     * for example, a point between floors or outside a setback floor's outline.
     */
    public Optional<Floor> findFloor(double x, double y, double z) {
        return floors.stream()
                .filter(floor -> floor.contains(x, y, z))
                .findFirst();
    }
}