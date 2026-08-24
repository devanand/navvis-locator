package com.navvis.locator.domain.port.out;

import com.navvis.locator.domain.model.geometry.Building;

import java.util.List;

/**
 * Parses raw building data (JSON bytes) into domain objects.
 *
 * Defined as an interface so the parsing technology (Jackson, Gson, etc.)
 * stays in the adapter layer.
 */
public interface BuildingDataParser {

    List<Building> parse(byte[] fileContent);
}