package com.navvis.locator.adapter.in.web.upload.controller;

import com.navvis.locator.domain.port.in.UploadBuildingsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuildingUploadController.class)
@AutoConfigureRestDocs
class BuildingUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadBuildingsUseCase uploadBuildingsUseCase;

    @Test
    @DisplayName("upload returns 200 with buildings created count")
    void upload() throws Exception {
        when(uploadBuildingsUseCase.submit(any(byte[].class))).thenReturn(3);

        MockMultipartFile file = new MockMultipartFile(
                "file", "buildings.json", "application/json",
                "test content".getBytes()
        );

        mockMvc.perform(multipart("/api/buildings/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildingsCreated").value(3))
                .andDo(document("upload/submit",
                        requestParts(
                                partWithName("file").description("JSON file containing building data")
                        ),
                        responseFields(
                                fieldWithPath("buildingsCreated").description("Number of buildings parsed and persisted")
                        )
                ));
    }

    @Test
    @DisplayName("upload without file returns 400")
    void uploadNoFile() throws Exception {
        mockMvc.perform(multipart("/api/buildings/upload")).andExpect(status().isBadRequest());
    }
}