package com.navvis.locator.adapter.in.web.location.controller;

import com.navvis.locator.adapter.in.web.location.dto.LocateRequest;
import com.navvis.locator.adapter.in.web.location.dto.LocateResponse;
import com.navvis.locator.domain.port.in.LocatePointUseCase;
import com.navvis.locator.domain.port.in.LocationResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LocationController {

    private final LocatePointUseCase locatePointUseCase;

    public LocationController(LocatePointUseCase locatePointUseCase) {
        this.locatePointUseCase = locatePointUseCase;
    }

    @PostMapping("/locate")
    public ResponseEntity<LocateResponse> locate(@Valid @RequestBody LocateRequest request) {
        LocationResult result = locatePointUseCase.locate(
                request.x(), request.y(), request.z()
        );

        LocateResponse response = switch (result) {
            case LocationResult.Located(var building, var floor) ->
                    new LocateResponse(building.name(), floor.name());
            case LocationResult.BuildingOnly(var building) ->
                    new LocateResponse(building.name(), null);
            case LocationResult.NotFound() ->
                    new LocateResponse(null, null);
        };

        return ResponseEntity.ok(response);
    }
}