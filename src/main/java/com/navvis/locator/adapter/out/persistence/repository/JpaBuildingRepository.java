package com.navvis.locator.adapter.out.persistence.repository;

import com.navvis.locator.application.strategy.LocateStrategy;
import com.navvis.locator.application.strategy.LocateStrategyToggle;
import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.port.out.BuildingRepository;
import com.navvis.locator.adapter.out.persistence.mapper.BuildingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaBuildingRepository implements BuildingRepository {

    private static final Logger LOG = LoggerFactory.getLogger(JpaBuildingRepository.class);

    private final SpringDataBuildingRepository springDataRepository;
    private final LocateStrategyToggle strategyToggle;

    public JpaBuildingRepository(
            SpringDataBuildingRepository springDataRepository,
            LocateStrategyToggle strategyToggle
    ) {
        this.springDataRepository = springDataRepository;
        this.strategyToggle = strategyToggle;
    }

    @Override
    public void saveAll(List<Building> buildings) {
        buildings.stream()
                .map(BuildingMapper::toEntity)
                .forEach(springDataRepository::save);
    }

    @Override
    public List<Building> findContaining(double x, double y, double z) {
        LocateStrategy strategy = strategyToggle.current();
        long start = System.nanoTime();

        List<Building> result = switch (strategy) {
            case JAVA -> findUsingJavaRayCasting(x, y, z);
            case POSTGIS -> findUsingPostgis(x, y, z);
        };

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        LOG.info("locate [{} strategy] ({}, {}, {}) -> {} match(es) in {}ms",
                strategy, x, y, z, result.size(), durationMs);

        return result;
    }

    private List<Building> findUsingJavaRayCasting(double x, double y, double z) {
        return springDataRepository
                .findAll(BuildingSpecifications.heightContains(z))
                .stream()
                .map(BuildingMapper::toDomain)
                .filter(building -> building.contains(x, y, z))
                .toList();
    }

    private List<Building> findUsingPostgis(double x, double y, double z) {
        return springDataRepository
                .findAll(BuildingSpecifications.spatialContains(x, y, z))
                .stream()
                .map(BuildingMapper::toDomain)
                .toList();
    }
}