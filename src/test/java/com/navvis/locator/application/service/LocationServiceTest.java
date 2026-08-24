package com.navvis.locator.application.service;

import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.model.geometry.Floor;
import com.navvis.locator.domain.model.geometry.HeightRange;
import com.navvis.locator.domain.model.geometry.Point2D;
import com.navvis.locator.domain.model.geometry.Polygon2D;
import com.navvis.locator.domain.port.in.LocationResult;
import com.navvis.locator.domain.port.out.BuildingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @InjectMocks
    private LocationService locationService;

    private static final Polygon2D OUTLINE = new Polygon2D(List.of(
            new Point2D(0, 0), new Point2D(20, 0),
            new Point2D(20, 20), new Point2D(0, 20),
            new Point2D(0, 0)
    ));

    private static final Floor GROUND_FLOOR = new Floor("Ground", OUTLINE, new HeightRange(0, 3));

    private static final Building BUILDING = new Building(
            "Office", OUTLINE, new HeightRange(0, 10),
            List.of(GROUND_FLOOR)
    );

    @Test
    @DisplayName("returns NotFound when no buildings match")
    void notFound() {
        when(buildingRepository.findContaining(5, 5, 5)).thenReturn(List.of());

        LocationResult result = locationService.locate(5, 5, 5);

        assertInstanceOf(LocationResult.NotFound.class, result);
    }

    @Test
    @DisplayName("returns Located when point is inside a building and on a floor")
    void located() {
        when(buildingRepository.findContaining(10, 10, 1)).thenReturn(List.of(BUILDING));

        LocationResult result = locationService.locate(10, 10, 1);

        assertInstanceOf(LocationResult.Located.class, result);
        LocationResult.Located located = (LocationResult.Located) result;
        assertEquals("Office", located.building().name());
        assertEquals("Ground", located.floor().name());
    }

    @Test
    @DisplayName("returns BuildingOnly when point is inside building but not on any floor")
    void buildingOnly() {
        // z=7 is within the building height (0-10) but above the ground floor (0-3)
        when(buildingRepository.findContaining(10, 10, 7)).thenReturn(List.of(BUILDING));

        LocationResult result = locationService.locate(10, 10, 7);

        assertInstanceOf(LocationResult.BuildingOnly.class, result);
        LocationResult.BuildingOnly buildingOnly = (LocationResult.BuildingOnly) result;
        assertEquals("Office", buildingOnly.building().name());
    }
}