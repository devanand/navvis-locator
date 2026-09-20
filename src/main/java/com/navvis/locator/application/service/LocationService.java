package com.navvis.locator.application.service;

import com.navvis.locator.application.strategy.BuildingLocatorResolver;
import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.model.geometry.Floor;
import com.navvis.locator.domain.port.in.LocatePointUseCase;
import com.navvis.locator.domain.port.in.LocationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LocationService implements LocatePointUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(LocationService.class);
    private final BuildingLocatorResolver locatorResolver;

    public LocationService(BuildingLocatorResolver locatorResolver) {
        this.locatorResolver = locatorResolver;
    }

    @Override
    public LocationResult locate(double x, double y, double z) {
        long start = System.nanoTime();
        List<Building> candidates = locatorResolver.current().locate(x, y, z);
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        LOG.info("locate [{} strategy] ({}, {}, {}) -> {} match(es) in {}ms",
                locatorResolver.current().type(), x, y, z, candidates.size(), durationMs);

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