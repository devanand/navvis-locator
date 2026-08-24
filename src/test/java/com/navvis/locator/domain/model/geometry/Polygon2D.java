package com.navvis.locator.domain.model.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Polygon2DTest {

    /**
     * A simple 10x10 square with vertices at (0,0), (10,0), (10,10), (0,10).
     * The first point is repeated at the end to close the polygon,
     * matching the format the JSON parser produces.
     */
    private static final Polygon2D SQUARE = new Polygon2D(List.of(
            new Point2D(0, 0),
            new Point2D(10, 0),
            new Point2D(10, 10),
            new Point2D(0, 10),
            new Point2D(0, 0)
    ));

    /**
     * An L-shaped polygon (concave) to test the ray casting algorithm
     * handles non-convex shapes correctly.
     *
     *   (0,10)-----(5,10)
     *     |           |
     *     |    (5,5)--(10,5)
     *     |    |         |
     *   (0,0)--(10,0)---+
     *
     * Wait -- let me define it properly:
     *   Vertices: (0,0) (10,0) (10,5) (5,5) (5,10) (0,10) (0,0)
     */
    private static final Polygon2D L_SHAPE = new Polygon2D(List.of(
            new Point2D(0, 0),
            new Point2D(10, 0),
            new Point2D(10, 5),
            new Point2D(5, 5),
            new Point2D(5, 10),
            new Point2D(0, 10),
            new Point2D(0, 0)
    ));

    @Nested
    @DisplayName("Square polygon")
    class SquarePolygon {

        @Test
        @DisplayName("point clearly inside returns true")
        void pointInside() {
            assertTrue(SQUARE.contains(new Point2D(5, 5)));
        }

        @Test
        @DisplayName("point clearly outside returns false")
        void pointOutside() {
            assertFalse(SQUARE.contains(new Point2D(15, 15)));
        }

        @Test
        @DisplayName("point to the left of the polygon returns false")
        void pointLeft() {
            assertFalse(SQUARE.contains(new Point2D(-1, 5)));
        }

        @Test
        @DisplayName("point above the polygon returns false")
        void pointAbove() {
            assertFalse(SQUARE.contains(new Point2D(5, 11)));
        }

        @Test
        @DisplayName("point near but outside a corner returns false")
        void pointNearCorner() {
            assertFalse(SQUARE.contains(new Point2D(-0.001, -0.001)));
        }

        @Test
        @DisplayName("point just inside the boundary returns true")
        void pointJustInside() {
            assertTrue(SQUARE.contains(new Point2D(0.001, 0.001)));
        }
    }

    @Nested
    @DisplayName("L-shaped (concave) polygon")
    class LShapedPolygon {

        @Test
        @DisplayName("point in the bottom arm of the L returns true")
        void pointInBottomArm() {
            assertTrue(L_SHAPE.contains(new Point2D(8, 2)));
        }

        @Test
        @DisplayName("point in the left arm of the L returns true")
        void pointInLeftArm() {
            assertTrue(L_SHAPE.contains(new Point2D(2, 8)));
        }

        @Test
        @DisplayName("point in the concave cutout returns false")
        void pointInCutout() {
            assertFalse(L_SHAPE.contains(new Point2D(8, 8)));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("triangle with three vertices plus closing point")
        void triangle() {
            Polygon2D triangle = new Polygon2D(List.of(
                    new Point2D(0, 0),
                    new Point2D(10, 0),
                    new Point2D(5, 10),
                    new Point2D(0, 0)
            ));
            assertTrue(triangle.contains(new Point2D(5, 3)));
            assertFalse(triangle.contains(new Point2D(0, 10)));
        }

        @Test
        @DisplayName("point with same y as a vertex does not cause division issues")
        void pointAtVertexHeight() {
            // y=0 is exactly on two vertices of the square;
            // the algorithm must not double-count or error
            assertFalse(SQUARE.contains(new Point2D(-1, 0)));
        }
    }
}