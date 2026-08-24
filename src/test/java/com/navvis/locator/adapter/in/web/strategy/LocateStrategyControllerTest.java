package com.navvis.locator.adapter.in.web.strategy;

import com.navvis.locator.application.strategy.LocateStrategy;
import com.navvis.locator.application.strategy.LocateStrategyToggle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocateStrategyController.class)
@AutoConfigureRestDocs
class LocateStrategyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocateStrategyToggle toggle;

    @Test
    @DisplayName("GET returns current strategy")
    void getCurrent() throws Exception {
        when(toggle.current()).thenReturn(LocateStrategy.JAVA);

        mockMvc.perform(get("/api/strategy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("JAVA"))
                .andDo(document("strategy/get",
                        responseFields(
                                fieldWithPath("strategy").description("Current locate strategy: JAVA or POSTGIS")
                        )
                ));
    }

    @Test
    @DisplayName("PUT switches strategy to POSTGIS")
    void switchToPostgis() throws Exception {
        when(toggle.current()).thenReturn(LocateStrategy.POSTGIS);

        mockMvc.perform(put("/api/strategy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strategy\":\"POSTGIS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("POSTGIS"))
                .andDo(document("strategy/switch",
                        requestFields(
                                fieldWithPath("strategy").description("Strategy to switch to: JAVA or POSTGIS")
                        ),
                        responseFields(
                                fieldWithPath("strategy").description("Active strategy after the switch")
                        )
                ));

        verify(toggle).switchTo(LocateStrategy.POSTGIS);
    }

    @Test
    @DisplayName("PUT with invalid strategy returns 400 Problem Detail")
    void switchInvalid() throws Exception {
        mockMvc.perform(put("/api/strategy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strategy\":\"INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed request body"));
    }
}