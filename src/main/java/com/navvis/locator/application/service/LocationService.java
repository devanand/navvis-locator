package com.navvis.locator.application.service;

import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.model.geometry.Floor;
import com.navvis.locator.domain.port.in.LocatePointUseCase;
import com.navvis.locator.domain.port.in.LocationResult;
import com.navvis.locator.domain.port.out.BuildingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LocationService implements LocatePointUseCase {

    private final BuildingRepository buildingRepository;

    public LocationService(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
    }

    @Override
    public LocationResult locate(double x, double y, double z) {
        // PostGIS does the heavy spatial filtering;
        // what comes back already passed ST_Contains + height range
        List<Building> candidates = buildingRepository.findContaining(x, y, z);

        if (candidates.isEmpty()) {
            return new LocationResult.NotFound();
        }

        Building building = candidates.getFirst();
        Optional<Floor> floor = building.findFloor(x, y, z);

        return floor
                .map(f -> (LocationResult) new LocationResult.Located(building, f))
                .orElse(new LocationResult.BuildingOnly(building));
    }
}