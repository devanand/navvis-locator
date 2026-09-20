package com.navvis.locator.adapter.in.web.location.mapper;

import com.navvis.locator.adapter.in.web.location.dto.LocateResponse;
import com.navvis.locator.domain.port.in.LocationResult;
import org.springframework.stereotype.Component;

/**
 * Visitor that maps each LocationResult variant to a LocateResponse DTO.
 *
 * <p>Lives in the web adapter layer, keeping the domain model free
 * of serialisation concerns. A new LocationResult variant will cause
 * a compile error here until it is handled.</p>
 */
@Component
public class LocationResponseMapper implements LocationResult.Visitor<LocateResponse> {

    @Override
    public LocateResponse visit(LocationResult.Located located) {
        return new LocateResponse(located.building().name(), located.floor().name());
    }

    @Override
    public LocateResponse visit(LocationResult.BuildingOnly buildingOnly) {
        return new LocateResponse(buildingOnly.building().name(), null);
    }

    @Override
    public LocateResponse visit(LocationResult.NotFound notFound) {
        return new LocateResponse(null, null);
    }
}