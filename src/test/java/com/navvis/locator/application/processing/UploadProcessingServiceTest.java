package com.navvis.locator.application.processing;

import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.model.geometry.HeightRange;
import com.navvis.locator.domain.model.geometry.Point2D;
import com.navvis.locator.domain.model.geometry.Polygon2D;
import com.navvis.locator.domain.port.out.BuildingDataParser;
import com.navvis.locator.domain.port.out.BuildingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadProcessingServiceTest {

    private static final byte[] FILE_CONTENT = "test data".getBytes();

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private BuildingDataParser parser;

    private UploadProcessingService service;

    @BeforeEach
    void setUp() {
        service = new UploadProcessingService(buildingRepository, parser);
    }

    @Test
    @DisplayName("parses file and persists buildings, returns count")
    void successfulProcessing() {
        Building building = new Building("Office",
                new Polygon2D(List.of(
                        new Point2D(0, 0), new Point2D(10, 0),
                        new Point2D(10, 10), new Point2D(0, 10),
                        new Point2D(0, 0))),
                new HeightRange(0, 10),
                List.of());
        when(parser.parse(FILE_CONTENT)).thenReturn(List.of(building));

        int count = service.process(FILE_CONTENT);

        assertEquals(1, count);
        verify(buildingRepository).saveAll(List.of(building));
    }

    @Test
    @DisplayName("propagates exception on invalid input")
    void failedProcessing() {
        when(parser.parse(FILE_CONTENT)).thenThrow(new RuntimeException("Invalid JSON"));

        assertThrows(RuntimeException.class, () -> service.process(FILE_CONTENT));
    }
}