package com.navvis.locator.adapter.out.persistence.repository;

import com.navvis.locator.adapter.out.persistence.entity.BuildingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SpringDataBuildingRepository extends
        JpaRepository<BuildingEntity, UUID>,
        JpaSpecificationExecutor<BuildingEntity> {
}