package com.navvis.locator.domain.model.geometry;

import java.util.List;

public record Polygon2D(List<Point2D> vertices) {

    /**
     * Determines whether a given 2D point lies inside this polygon
     * using the ray casting algorithm.
     *
     * The algorithm shoots an imaginary ray from the point horizontally
     * to the right and counts how many times it crosses a polygon edge.
     * Odd crossings = inside, even crossings = outside.
     */
    public boolean contains(Point2D point) {
        int intersections = 0;
        int n = vertices.size();

        // Iterate over each edge (defined by consecutive vertex pairs)
        // The JSON outline closes the polygon by repeating the first point,
        // so n-1 iterations covers all edges exactly once
        for (int i = 0; i < n - 1; i++) {
            Point2D a = vertices.get(i);
            Point2D b = vertices.get(i + 1);

            if (rayIntersectsEdge(point, a, b)) {
                intersections++;
            }
        }
        // Odd number of crossings means the point is inside the polygon
        return intersections % 2 == 1;
    }

    /**
     * Checks whether a horizontal ray fired rightward from the given point
     * intersects the edge defined by vertices a and b.
     */
    private boolean rayIntersectsEdge(Point2D point, Point2D a, Point2D b) {
        // If both endpoints are on the same side of the ray's y-level,
        // the ray cannot cross this edge — skip it
        if ((a.y() > point.y()) == (b.y() > point.y())) return false;

        // Calculate the x-coordinate where the ray would cross this edge
        // and check if the point is to the left of that crossing
        double xIntersect = a.x() + (point.y() - a.y()) / (b.y() - a.y()) * (b.x() - a.x());
        return point.x() < xIntersect;
    }
}