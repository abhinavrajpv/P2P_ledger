package com.p2pledger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2pledger.service.MaskingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaskingController.class)
class MaskingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MaskingService maskingService;

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetStatus_Enabled() throws Exception {
        // Arrange
        MaskingService.MaskingStatus status = new MaskingService.MaskingStatus(true);
        when(maskingService.getStatus(1L)).thenReturn(status);

        // Act & Assert
        mockMvc.perform(get("/api/masking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(maskingService).getStatus(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetStatus_Disabled() throws Exception {
        // Arrange
        MaskingService.MaskingStatus status = new MaskingService.MaskingStatus(false);
        when(maskingService.getStatus(1L)).thenReturn(status);

        // Act & Assert
        mockMvc.perform(get("/api/masking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        verify(maskingService).getStatus(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testEnableMasking_Success() throws Exception {
        // Arrange
        MaskingController.PinRequest request = new MaskingController.PinRequest();
        request.setPin("1234");

        MaskingService.MaskingStatus status = new MaskingService.MaskingStatus(true);
        when(maskingService.enableMasking(1L, "1234")).thenReturn(status);

        // Act & Assert
        mockMvc.perform(post("/api/masking/enable")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(maskingService).enableMasking(1L, "1234");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testEnableMasking_InvalidPin() throws Exception {
        // Arrange
        MaskingController.PinRequest request = new MaskingController.PinRequest();
        request.setPin("0000");

        when(maskingService.enableMasking(1L, "0000"))
                .thenThrow(new IllegalArgumentException("Invalid transaction PIN"));

        // Act & Assert
        mockMvc.perform(post("/api/masking/enable")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(maskingService).enableMasking(1L, "0000");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testDisableMasking_Success() throws Exception {
        // Arrange
        MaskingController.PinRequest request = new MaskingController.PinRequest();
        request.setPin("1234");

        MaskingService.MaskingStatus status = new MaskingService.MaskingStatus(false);
        when(maskingService.disableMasking(1L, "1234")).thenReturn(status);

        // Act & Assert
        mockMvc.perform(post("/api/masking/disable")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        verify(maskingService).disableMasking(1L, "1234");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testDisableMasking_InvalidPin() throws Exception {
        // Arrange
        MaskingController.PinRequest request = new MaskingController.PinRequest();
        request.setPin("9999");

        when(maskingService.disableMasking(1L, "9999"))
                .thenThrow(new IllegalArgumentException("Invalid transaction PIN"));

        // Act & Assert
        mockMvc.perform(post("/api/masking/disable")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(maskingService).disableMasking(1L, "9999");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testEnableMasking_AlreadyEnabled() throws Exception {
        // Arrange
        MaskingController.PinRequest request = new MaskingController.PinRequest();
        request.setPin("1234");

        when(maskingService.enableMasking(1L, "1234"))
                .thenThrow(new IllegalArgumentException("Masking already enabled"));

        // Act & Assert
        mockMvc.perform(post("/api/masking/enable")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(maskingService).enableMasking(1L, "1234");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testDisableMasking_AlreadyDisabled() throws Exception {
        // Arrange
        MaskingController.PinRequest request = new MaskingController.PinRequest();
        request.setPin("1234");

        when(maskingService.disableMasking(1L, "1234"))
                .thenThrow(new IllegalArgumentException("Masking already disabled"));

        // Act & Assert
        mockMvc.perform(post("/api/masking/disable")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(maskingService).disableMasking(1L, "1234");
    }
}
