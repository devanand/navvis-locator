package com.navvis.locator.adapter.out.persistence.repository;

import com.navvis.locator.adapter.out.persistence.entity.BuildingEntity;
import com.navvis.locator.adapter.out.persistence.mapper.BuildingMapper;
import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.port.out.BuildingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaBuildingRepository implements BuildingRepository {

    private final SpringDataBuildingRepository springDataRepository;

    public JpaBuildingRepository(SpringDataBuildingRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public void saveAll(List<Building> buildings) {
        List<BuildingEntity> entities = buildings.stream()
                .map(BuildingMapper::toEntity)
                .toList();
        springDataRepository.saveAll(entities);
    }

    @Override
    public List<Building> findByHeightRange(double z) {
        return springDataRepository.findAll(BuildingSpecifications.heightContains(z)).stream()
                .map(BuildingMapper::toDomain)
                .toList();
    }

    @Override
    public List<Building> findSpatialContaining(double x, double y, double z) {
        return springDataRepository.findAll(BuildingSpecifications.spatialContains(x, y, z)).stream()
                .map(BuildingMapper::toDomain)
                .toList();
    }
}