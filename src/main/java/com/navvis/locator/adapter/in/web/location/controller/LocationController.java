package com.navvis.locator.adapter.in.web.location.controller;

import com.navvis.locator.adapter.in.web.location.dto.LocateRequest;
import com.navvis.locator.adapter.in.web.location.dto.LocateResponse;
import com.navvis.locator.adapter.in.web.location.mapper.LocationResponseMapper;
import com.navvis.locator.domain.port.in.LocatePointUseCase;
import com.navvis.locator.domain.port.in.LocationResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LocationController {

    private final LocatePointUseCase locatePointUseCase;
    private final LocationResponseMapper responseMapper;

    public LocationController(LocatePointUseCase locatePointUseCase, LocationResponseMapper responseMapper) {
        this.locatePointUseCase = locatePointUseCase;
        this.responseMapper = responseMapper;
    }

    @PostMapping("/locate")
    public ResponseEntity<LocateResponse> locate(@Valid @RequestBody LocateRequest request) {
        LocationResult result = locatePointUseCase.locate(
                request.x(), request.y(), request.z()
        );
        return ResponseEntity.ok(result.accept(responseMapper));
    }
}