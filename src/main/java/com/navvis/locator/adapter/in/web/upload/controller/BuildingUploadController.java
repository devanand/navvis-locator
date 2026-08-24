package com.navvis.locator.adapter.in.web.upload.controller;

import com.navvis.locator.adapter.in.web.upload.dto.UploadResponse;
import com.navvis.locator.domain.port.in.UploadBuildingsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/buildings")
public class BuildingUploadController {

    private final UploadBuildingsUseCase uploadBuildingsUseCase;

    public BuildingUploadController(UploadBuildingsUseCase uploadBuildingsUseCase) {
        this.uploadBuildingsUseCase = uploadBuildingsUseCase;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file)
            throws IOException {
        int buildingsCreated = uploadBuildingsUseCase.submit(file.getBytes());
        return ResponseEntity.ok(new UploadResponse(buildingsCreated));
    }
}