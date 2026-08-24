package com.navvis.locator.domain.model.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingTest {

    private static final Polygon2D BUILDING_OUTLINE = new Polygon2D(List.of(
            new Point2D(0, 0),
            new Point2D(20, 0),
            new Point2D(20, 20),
            new Point2D(0, 20),
            new Point2D(0, 0)
    ));

    /** A smaller "setback" outline, like Floor 4 in the example data. */
    private static final Polygon2D SETBACK_OUTLINE = new Polygon2D(List.of(
            new Point2D(5, 5),
            new Point2D(15, 5),
            new Point2D(15, 15),
            new Point2D(5, 15),
            new Point2D(5, 5)
    ));

    private static final Floor GROUND_FLOOR = new Floor("Ground", BUILDING_OUTLINE, new HeightRange(0, 3));
    private static final Floor FIRST_FLOOR = new Floor("First", BUILDING_OUTLINE, new HeightRange(3, 6));
    private static final Floor SETBACK_FLOOR = new Floor("Setback", SETBACK_OUTLINE, new HeightRange(6, 9));

    private static final Building BUILDING = new Building(
            "Test Building",
            BUILDING_OUTLINE,
            new HeightRange(0, 9),
            List.of(GROUND_FLOOR, FIRST_FLOOR, SETBACK_FLOOR)
    );

    @Test
    @DisplayName("point inside outline and within height range is contained")
    void containsPointInside() {
        assertTrue(BUILDING.contains(10, 10, 1));
    }

    @Test
    @DisplayName("point inside outline but above height range is not contained")
    void rejectsPointAboveHeight() {
        assertFalse(BUILDING.contains(10, 10, 20));
    }

    @Test
    @DisplayName("point outside outline but within height range is not contained")
    void rejectsPointOutsideOutline() {
        assertFalse(BUILDING.contains(25, 25, 1));
    }

    @Test
    @DisplayName("findFloor returns the correct floor for a point on the ground floor")
    void findsGroundFloor() {
        Optional<Floor> result = BUILDING.findFloor(10, 10, 1);
        assertTrue(result.isPresent());
        assertEquals("Ground", result.get().name());
    }

    @Test
    @DisplayName("findFloor returns the correct floor for a point on the first floor")
    void findsFirstFloor() {
        Optional<Floor> result = BUILDING.findFloor(10, 10, 4);
        assertTrue(result.isPresent());
        assertEquals("First", result.get().name());
    }

    @Test
    @DisplayName("findFloor returns the setback floor when point is within its smaller outline")
    void findsSetbackFloor() {
        Optional<Floor> result = BUILDING.findFloor(10, 10, 7);
        assertTrue(result.isPresent());
        assertEquals("Setback", result.get().name());
    }

    @Test
    @DisplayName("findFloor returns empty when point is inside building but outside setback outline")
    void emptyForPointOutsideSetback() {
        // (2, 2) is inside the building outline but outside the setback floor's (5,5)-(15,15) outline
        Optional<Floor> result = BUILDING.findFloor(2, 2, 7);
        assertTrue(result.isEmpty());
    }
}