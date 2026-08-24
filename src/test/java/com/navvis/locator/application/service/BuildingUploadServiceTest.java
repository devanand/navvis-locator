package com.navvis.locator.application.service;

import com.navvis.locator.application.processing.UploadProcessingService;
import com.navvis.locator.domain.port.in.UploadBuildingsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildingUploadServiceTest {

    private static final byte[] FILE_CONTENT = "test data".getBytes();

    @Mock
    private UploadProcessingService processingService;

    private UploadBuildingsUseCase service;

    @BeforeEach
    void setUp() {
        service = new BuildingUploadService(processingService);
    }

    @Test
    @DisplayName("delegates to processing service and returns building count")
    void submit() {
        when(processingService.process(FILE_CONTENT)).thenReturn(3);

        int count = service.submit(FILE_CONTENT);

        assertEquals(3, count);
        verify(processingService).process(FILE_CONTENT);
    }
}