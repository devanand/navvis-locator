package com.navvis.locator.domain.port.in;

import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.model.geometry.Floor;

/**
 * The outcome of locating a 3D point.
 *
 * Modelled as a sealed interface because there are exactly three
 * distinct outcomes, and making them explicit prevents the
 * "building found but floor is null" ambiguity that a single
 * nullable record would introduce.
 */
public sealed interface LocationResult {

    /** Point is inside a building and on one of its floors. */
    record Located(Building building, Floor floor) implements LocationResult {}

    /**
     * Point is inside the building envelope but not on any floor —
     * e.g. it falls in the gap between floors, or outside the
     * outline of a setback floor such as "Floor 4" in the sample data.
     */
    record BuildingOnly(Building building) implements LocationResult {}

    /** Point is not inside any known building. */
    record NotFound() implements LocationResult {}
}