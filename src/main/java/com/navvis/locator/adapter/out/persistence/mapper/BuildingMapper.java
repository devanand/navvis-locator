package com.navvis.locator.adapter.out.persistence.mapper;

import com.navvis.locator.adapter.out.persistence.entity.BuildingEntity;
import com.navvis.locator.adapter.out.persistence.entity.FloorEntity;
import com.navvis.locator.domain.model.geometry.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

public class BuildingMapper {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    // ── Domain → Entity ──

    public static BuildingEntity toEntity(Building building) {
        BuildingEntity entity = new BuildingEntity();
        entity.setName(building.name());
        entity.setOutline(toJtsPolygon(building.outline()));
        entity.setHeightMin(building.height().min());
        entity.setHeightMax(building.height().max());

        List<FloorEntity> floorEntities = building.floors().stream()
                .map(floor -> toFloorEntity(floor, entity))
                .toList();
        entity.setFloors(floorEntities);

        return entity;
    }

    private static FloorEntity toFloorEntity(Floor floor, BuildingEntity parent) {
        FloorEntity entity = new FloorEntity();
        entity.setName(floor.name());
        entity.setOutline(toJtsPolygon(floor.outline()));
        entity.setHeightMin(floor.height().min());
        entity.setHeightMax(floor.height().max());
        entity.setBuilding(parent);
        return entity;
    }

    // ── Entity → Domain ──

    public static Building toDomain(BuildingEntity entity) {
        List<Floor> floors = entity.getFloors().stream()
                .map(BuildingMapper::toFloorDomain)
                .toList();

        return new Building(
                entity.getName(),
                toPolygon2D(entity.getOutline()),
                new HeightRange(entity.getHeightMin(), entity.getHeightMax()),
                floors
        );
    }

    private static Floor toFloorDomain(FloorEntity entity) {
        return new Floor(
                entity.getName(),
                toPolygon2D(entity.getOutline()),
                new HeightRange(entity.getHeightMin(), entity.getHeightMax())
        );
    }

    // ── Geometry conversions ──

    /**
     * Converts our domain Polygon2D (list of Point2D) to a JTS Polygon
     * that PostGIS understands.
     */
    private static Polygon toJtsPolygon(Polygon2D polygon) {
        Coordinate[] coordinates = polygon.vertices().stream()
                .map(p -> new Coordinate(p.x(), p.y()))
                .toArray(Coordinate[]::new);

        Polygon jtsPolygon = GEOMETRY_FACTORY.createPolygon(coordinates);
        jtsPolygon.setSRID(0);
        return jtsPolygon;
    }

    /**
     * Converts a JTS Polygon back to our domain Polygon2D.
     */
    private static Polygon2D toPolygon2D(Polygon polygon) {
        List<Point2D> vertices = List.of(polygon.getCoordinates()).stream()
                .map(c -> new Point2D(c.x, c.y))
                .toList();

        return new Polygon2D(vertices);
    }
}