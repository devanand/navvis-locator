package com.navvis.locator.application.strategy;

import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.port.out.BuildingRepository;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class JavaRayCastingLocator implements BuildingLocator {

    private final BuildingRepository repository;

    public JavaRayCastingLocator(BuildingRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Building> locate(double x, double y, double z) {
        return repository.findByHeightRange(z).stream()
                .filter(b -> b.contains(x, y, z))
                .toList();
    }

    @Override
    public LocateStrategy type() {
        return LocateStrategy.JAVA;
    }
}