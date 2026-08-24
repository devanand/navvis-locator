package com.navvis.locator.application.processing;

import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.port.out.BuildingDataParser;
import com.navvis.locator.domain.port.out.BuildingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UploadProcessingService {

    private final BuildingRepository buildingRepository;
    private final BuildingDataParser parser;

    public UploadProcessingService(
            BuildingRepository buildingRepository,
            BuildingDataParser parser
    ) {
        this.buildingRepository = buildingRepository;
        this.parser = parser;
    }

    public int process(byte[] fileContent) {
        List<Building> buildings = parser.parse(fileContent);
        buildingRepository.saveAll(buildings);
        return buildings.size();
    }
}