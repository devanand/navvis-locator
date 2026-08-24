package com.navvis.locator.application.service;

import com.navvis.locator.application.processing.UploadProcessingService;
import com.navvis.locator.domain.port.in.UploadBuildingsUseCase;
import org.springframework.stereotype.Service;

@Service
public class BuildingUploadService implements UploadBuildingsUseCase {

    private final UploadProcessingService processingService;

    public BuildingUploadService(UploadProcessingService processingService) {
        this.processingService = processingService;
    }

    @Override
    public int submit(byte[] fileContent) {
        return processingService.process(fileContent);
    }
}