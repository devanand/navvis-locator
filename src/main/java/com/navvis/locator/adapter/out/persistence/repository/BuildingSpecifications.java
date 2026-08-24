package com.navvis.locator.adapter.out.persistence.repository;

import com.navvis.locator.adapter.out.persistence.entity.BuildingEntity;
import com.navvis.locator.adapter.out.persistence.entity.BuildingEntity_;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.Specification;

final class BuildingSpecifications {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private BuildingSpecifications() {}

    static Specification<BuildingEntity> heightContains(double z) {
        return (root, query, cb) -> cb.and(
                cb.lessThanOrEqualTo(root.get(BuildingEntity_.heightMin), z),
                cb.greaterThanOrEqualTo(root.get(BuildingEntity_.heightMax), z)
        );
    }

    static Specification<BuildingEntity> spatialContains(double x, double y, double z) {
        return (root, query, cb) -> {
            Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(x, y));
            return cb.and(
                    cb.isTrue(cb.function("ST_Covers", Boolean.class,
                            root.get(BuildingEntity_.outline), cb.literal(point))),
                    cb.lessThanOrEqualTo(root.get(BuildingEntity_.heightMin), z),
                    cb.greaterThanOrEqualTo(root.get(BuildingEntity_.heightMax), z)
            );
        };
    }
}