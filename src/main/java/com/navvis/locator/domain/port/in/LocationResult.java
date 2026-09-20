package com.navvis.locator.domain.port.in;

import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.model.geometry.Floor;

/**
 * Result of locating a 3D point within the building dataset.
 *
 * <p>Sealed to guarantee exhaustive handling. Uses the Visitor pattern
 * so mapping logic lives in the adapter layer, not scattered across
 * switch statements. Adding a new variant forces every Visitor
 * implementation to handle it at compile time.</p>
 */
public sealed interface LocationResult {

    /**
     * Dispatch to the appropriate visitor method based on the concrete variant.
     *
     * @param visitor the visitor that defines how each variant is handled
     * @param <T>     the return type of the visitor
     * @return the result produced by the visitor
     */
    <T> T accept(Visitor<T> visitor);

    /**
     * Defines one method per variant. Implementing a new variant without
     * updating every visitor is a compile error.
     */
    interface Visitor<T> {
        T visit(Located located);
        T visit(BuildingOnly buildingOnly);
        T visit(NotFound notFound);
    }

    /** Point matched both a building and a specific floor. */
    record Located(Building building, Floor floor) implements LocationResult {
        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    /** Point is inside a building but does not fall on any defined floor. */
    record BuildingOnly(Building building) implements LocationResult {
        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }

    /** Point is not inside any known building. */
    record NotFound() implements LocationResult {
        @Override
        public <T> T accept(Visitor<T> visitor) { return visitor.visit(this); }
    }
}