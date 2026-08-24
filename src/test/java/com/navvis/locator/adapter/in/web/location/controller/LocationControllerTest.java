package com.navvis.locator.adapter.in.web.location.controller;

import com.navvis.locator.domain.model.geometry.Building;
import com.navvis.locator.domain.model.geometry.Floor;
import com.navvis.locator.domain.model.geometry.HeightRange;
import com.navvis.locator.domain.model.geometry.Point2D;
import com.navvis.locator.domain.model.geometry.Polygon2D;
import com.navvis.locator.domain.port.in.LocatePointUseCase;
import com.navvis.locator.domain.port.in.LocationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@AutoConfigureRestDocs
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocatePointUseCase locatePointUseCase;

    private static final Polygon2D OUTLINE = new Polygon2D(List.of(
            new Point2D(0, 0), new Point2D(10, 0),
            new Point2D(10, 10), new Point2D(0, 10),
            new Point2D(0, 0)
    ));

    private static final Building BUILDING = new Building(
            "Office", OUTLINE, new HeightRange(0, 10), List.of()
    );

    private static final Floor FLOOR = new Floor("Ground", OUTLINE, new HeightRange(0, 3));

    @Test
    @DisplayName("returns building and floor when point is Located")
    void located() throws Exception {
        when(locatePointUseCase.locate(5, 5, 1))
                .thenReturn(new LocationResult.Located(BUILDING, FLOOR));

        mockMvc.perform(post("/api/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"x": 5, "y": 5, "z": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.building").value("Office"))
                .andExpect(jsonPath("$.floor").value("Ground"))
                .andDo(document("locate/found",
                        requestFields(
                                fieldWithPath("x").description("X coordinate of the query point"),
                                fieldWithPath("y").description("Y coordinate of the query point"),
                                fieldWithPath("z").description("Z coordinate (height) of the query point")
                        ),
                        responseFields(
                                fieldWithPath("building").description("Name of the building containing the point"),
                                fieldWithPath("floor").description("Name of the floor containing the point, or null if between floors")
                        )
                ));
    }

    @Test
    @DisplayName("returns building with null floor when BuildingOnly")
    void buildingOnly() throws Exception {
        when(locatePointUseCase.locate(5, 5, 7))
                .thenReturn(new LocationResult.BuildingOnly(BUILDING));

        mockMvc.perform(post("/api/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"x": 5, "y": 5, "z": 7}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.building").value("Office"))
                .andExpect(jsonPath("$.floor").doesNotExist())
                .andDo(document("locate/building-only",
                        responseFields(
                                fieldWithPath("building").description("Name of the building containing the point"),
                                fieldWithPath("floor").description("Null when the point is inside the building envelope but not on any floor").optional()
                        )
                ));
    }

    @Test
    @DisplayName("returns nulls when NotFound")
    void notFound() throws Exception {
        when(locatePointUseCase.locate(99, 99, 99))
                .thenReturn(new LocationResult.NotFound());

        mockMvc.perform(post("/api/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"x": 99, "y": 99, "z": 99}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.building").doesNotExist())
                .andExpect(jsonPath("$.floor").doesNotExist())
                .andDo(document("locate/not-found",
                        responseFields(
                                fieldWithPath("building").description("Null when the point is not inside any building").optional(),
                                fieldWithPath("floor").description("Null when the point is not inside any building").optional()
                        )
                ));
    }

    @Test
    @DisplayName("returns 400 when x is missing")
    void missingCoordinate() throws Exception {
        mockMvc.perform(post("/api/locate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"y": 5, "z": 1}
                                """))
                .andExpect(status().isBadRequest());
    }
}