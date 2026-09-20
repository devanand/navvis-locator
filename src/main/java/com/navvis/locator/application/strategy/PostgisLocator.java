package com.navvis.locator.application.strategy;

import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.port.out.BuildingRepository;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PostgisLocator implements BuildingLocator {

    private final BuildingRepository repository;

    public PostgisLocator(BuildingRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Building> locate(double x, double y, double z) {
        return repository.findSpatialContaining(x, y, z);
    }

    @Override
    public LocateStrategy type() {
        return LocateStrategy.POSTGIS;
    }
}