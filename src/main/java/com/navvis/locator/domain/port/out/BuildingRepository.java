package com.navvis.locator.domain.port.out;

import com.navvis.locator.domain.model.geometry.Building;

import java.util.List;

/**
 * Port for persisting and querying buildings.
 *
 * The implementation decides how — PostGIS, in-memory, etc.
 * The domain doesn't know or care.
 */
public interface BuildingRepository {

    void saveAll(List<Building> buildings);

    List<Building> findContaining(double x, double y, double z);
}