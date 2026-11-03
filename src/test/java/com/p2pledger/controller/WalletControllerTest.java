package com.p2pledger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2pledger.model.Role;
import com.p2pledger.model.Wallet;
import com.p2pledger.model.WalletStatus;
import com.p2pledger.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetBalance_Success() throws Exception {
        // Arrange
        WalletService.WalletBalanceResponse response = WalletService.WalletBalanceResponse.builder()
                .balance(BigDecimal.valueOf(1000))
                .todaySpending(BigDecimal.valueOf(500))
                .dailyLimit(BigDecimal.valueOf(50000))
                .status(WalletStatus.ACTIVE)
                .build();
        when(walletService.getBalance(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/wallet/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.todaySpending").value(500));

        verify(walletService).getBalance(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetBalance_FrozenWallet() throws Exception {
        // Arrange
        WalletService.WalletBalanceResponse response = WalletService.WalletBalanceResponse.builder()
                .balance(BigDecimal.valueOf(1000))
                .todaySpending(BigDecimal.valueOf(0))
                .dailyLimit(BigDecimal.valueOf(50000))
                .status(WalletStatus.FROZEN)
                .build();
        when(walletService.getBalance(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/wallet/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        verify(walletService).getBalance(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testDeposit_Success() throws Exception {
        // Arrange
        WalletController.DepositRequest request = new WalletController.DepositRequest();
        request.setAmount(BigDecimal.valueOf(500));
        request.setReference("DEP123");

        WalletService.WalletDepositResponse response = WalletService.WalletDepositResponse.builder()
                .id(1L)
                .amount(BigDecimal.valueOf(500))
                .newBalance(BigDecimal.valueOf(1500))
                .reference("DEP123")
                .build();
        when(walletService.deposit(1L, BigDecimal.valueOf(500), "DEP123")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/wallet/deposit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newBalance").value(1500))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.reference").value("DEP123"));

        verify(walletService).deposit(1L, BigDecimal.valueOf(500), "DEP123");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testDeposit_InvalidAmount() throws Exception {
        // Arrange
        WalletController.DepositRequest request = new WalletController.DepositRequest();
        request.setAmount(BigDecimal.valueOf(-100));
        request.setReference("DEP124");

        when(walletService.deposit(1L, BigDecimal.valueOf(-100), "DEP124"))
                .thenThrow(new IllegalArgumentException("Amount must be positive"));

        // Act & Assert
        mockMvc.perform(post("/api/wallet/deposit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(walletService).deposit(1L, BigDecimal.valueOf(-100), "DEP124");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testDeposit_DuplicateReference() throws Exception {
        // Arrange
        WalletController.DepositRequest request = new WalletController.DepositRequest();
        request.setAmount(BigDecimal.valueOf(500));
        request.setReference("DUP123");

        when(walletService.deposit(1L, BigDecimal.valueOf(500), "DUP123"))
                .thenThrow(new IllegalArgumentException("Duplicate deposit reference"));

        // Act & Assert
        mockMvc.perform(post("/api/wallet/deposit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(walletService).deposit(1L, BigDecimal.valueOf(500), "DUP123");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testUpdateStatus_AsUser() throws Exception {
        // Arrange
        Wallet wallet = new Wallet();
        wallet.setStatus(WalletStatus.ACTIVE);
        
        WalletController.StatusUpdateRequest request = new WalletController.StatusUpdateRequest();
        request.setStatus(WalletStatus.ACTIVE);

        WalletService.WalletStatusResponse response = WalletService.WalletStatusResponse.builder()
                .wallet(wallet)
                .build();
        when(walletService.updateStatus(1L, WalletStatus.ACTIVE, Role.USER)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/wallet/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet.status").value("ACTIVE"));

        verify(walletService).updateStatus(1L, WalletStatus.ACTIVE, Role.USER);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testUpdateStatus_AsAdmin() throws Exception {
        // Arrange
        Wallet wallet = new Wallet();
        wallet.setStatus(WalletStatus.FROZEN);
        
        WalletController.StatusUpdateRequest request = new WalletController.StatusUpdateRequest();
        request.setStatus(WalletStatus.FROZEN);

        WalletService.WalletStatusResponse response = WalletService.WalletStatusResponse.builder()
                .wallet(wallet)
                .build();
        when(walletService.updateStatus(1L, WalletStatus.FROZEN, Role.ADMIN)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/wallet/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet.status").value("FROZEN"));

        verify(walletService).updateStatus(1L, WalletStatus.FROZEN, Role.ADMIN);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testUpdateStatus_Forbidden() throws Exception {
        // Arrange
        WalletController.StatusUpdateRequest request = new WalletController.StatusUpdateRequest();
        request.setStatus(WalletStatus.FROZEN);

        when(walletService.updateStatus(1L, WalletStatus.FROZEN, Role.USER))
                .thenThrow(new IllegalArgumentException("Only admins can freeze wallets"));

        // Act & Assert
        mockMvc.perform(patch("/api/wallet/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(walletService).updateStatus(1L, WalletStatus.FROZEN, Role.USER);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetBalance_WalletNotFound() throws Exception {
        // Arrange
        when(walletService.getBalance(1L))
                .thenThrow(new IllegalArgumentException("Wallet not found"));

        // Act & Assert
        mockMvc.perform(get("/api/wallet/balance"))
                .andExpect(status().isBadRequest());

        verify(walletService).getBalance(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testDeposit_ZeroAmount() throws Exception {
        // Arrange
        WalletController.DepositRequest request = new WalletController.DepositRequest();
        request.setAmount(BigDecimal.ZERO);
        request.setReference("ZERO123");

        when(walletService.deposit(1L, BigDecimal.ZERO, "ZERO123"))
                .thenThrow(new IllegalArgumentException("Amount must be positive"));

        // Act & Assert
        mockMvc.perform(post("/api/wallet/deposit")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(walletService).deposit(1L, BigDecimal.ZERO, "ZERO123");
    }
}
