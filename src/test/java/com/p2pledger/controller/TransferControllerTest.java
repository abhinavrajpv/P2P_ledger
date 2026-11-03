package com.p2pledger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2pledger.model.*;
import com.p2pledger.service.TransferService;
import com.p2pledger.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferService transferService;

    @MockBean
    private UserService userService;

    private Transfer testTransfer;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setRole(Role.USER);

        testTransfer = Transfer.builder()
                .id(1L)
                .senderId(1L)
                .receiverId(2L)
                .amount(BigDecimal.valueOf(500))
                .status(TransferStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .settlementDate(LocalDateTime.now().plusDays(2))
                .requestId("REQ123")
                .build();
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testCreateTransfer_Success() throws Exception {
        // Arrange
        TransferController.CreateTransferRequest request = new TransferController.CreateTransferRequest();
        request.setReceiverId(2L);
        request.setAmount(BigDecimal.valueOf(500));
        request.setRequestId("REQ123");
        request.setPin("1234");

        when(transferService.initiateTransfer(1L, 2L, BigDecimal.valueOf(500), "REQ123", "1234"))
                .thenReturn(testTransfer);

        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(transferService).initiateTransfer(1L, 2L, BigDecimal.valueOf(500), "REQ123", "1234");
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testCreateTransfer_InvalidPin() throws Exception {
        // Arrange
        TransferController.CreateTransferRequest request = new TransferController.CreateTransferRequest();
        request.setReceiverId(2L);
        request.setAmount(BigDecimal.valueOf(500));
        request.setRequestId("REQ123");
        request.setPin("0000");

        when(transferService.initiateTransfer(1L, 2L, BigDecimal.valueOf(500), "REQ123", "0000"))
                .thenThrow(new IllegalArgumentException("Invalid transaction PIN"));

        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testCreateTransfer_InsufficientFunds() throws Exception {
        // Arrange
        TransferController.CreateTransferRequest request = new TransferController.CreateTransferRequest();
        request.setReceiverId(2L);
        request.setAmount(BigDecimal.valueOf(10000));
        request.setRequestId("REQ124");
        request.setPin("1234");

        when(transferService.initiateTransfer(1L, 2L, BigDecimal.valueOf(10000), "REQ124", "1234"))
                .thenThrow(new IllegalArgumentException("Insufficient funds"));

        // Act & Assert
        mockMvc.perform(post("/api/transfers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetTransfer_Success() throws Exception {
        // Arrange
        TransferService.TransferDetailDto detailDto = TransferService.TransferDetailDto.builder()
                .id(1L)
                .senderId(1L)
                .receiverId(2L)
                .amount(BigDecimal.valueOf(500))
                .requestId("REQ123")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .settlementDate(LocalDateTime.now().plusDays(2))
                .fraudFlagged(false)
                .adminReviewStatus("NOT_REQUIRED")
                .senderName("Sender")
                .receiverName("Receiver")
                .ledgerEntries(new ArrayList<>())
                .build();
        when(transferService.getTransferDetail(1L, 1L)).thenReturn(detailDto);

        // Act & Assert
        mockMvc.perform(get("/api/transfers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.senderName").value("Sender"))
                .andExpect(jsonPath("$.receiverName").value("Receiver"));

        verify(transferService).getTransferDetail(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetTransfer_NotFound() throws Exception {
        // Arrange
        when(transferService.getTransferDetail(999L, 1L))
                .thenThrow(new IllegalArgumentException("Transfer not found"));

        // Act & Assert
        mockMvc.perform(get("/api/transfers/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetTransactionHistory_Success() throws Exception {
        // Arrange
        TransferService.TransferHistoryDto historyDto = TransferService.TransferHistoryDto.builder()
                .id(1L)
                .amount(BigDecimal.valueOf(500))
                .requestId("REQ123")
                .type("DEBIT")
                .status(com.p2pledger.model.TransferStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .settlementDate(LocalDateTime.now().plusDays(2))
                .fraudFlagged(false)
                .adminReviewStatus(com.p2pledger.model.AdminReviewStatus.NOT_REQUIRED)
                .receiverName("Receiver")
                .daysUntilSettlement(2)
                .build();
        Page<TransferService.TransferHistoryDto> page = new PageImpl<>(Arrays.asList(historyDto));
        when(transferService.getTransactionHistory(eq(1L), anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/transfers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].type").value("DEBIT"))
                .andExpect(jsonPath("$.content[0].amount").value(500));

        verify(transferService).getTransactionHistory(eq(1L), anyInt(), anyInt(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetTransactionHistory_WithFilters() throws Exception {
        // Arrange
        Page<TransferService.TransferHistoryDto> page = new PageImpl<>(Arrays.asList());
        when(transferService.getTransactionHistory(eq(1L), anyInt(), anyInt(), eq("SENT"), eq("SUCCESS"), any(), any()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/transfers")
                .param("type", "SENT")
                .param("status", "SUCCESS"))
                .andExpect(status().isOk());

        verify(transferService).getTransactionHistory(eq(1L), anyInt(), anyInt(), eq("SENT"), eq("SUCCESS"), any(), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetTransactionHistory_WithDateRange() throws Exception {
        // Arrange
        Page<TransferService.TransferHistoryDto> page = new PageImpl<>(Arrays.asList());
        when(transferService.getTransactionHistory(eq(1L), anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/transfers")
                .param("startDate", "2024-01-01")
                .param("endDate", "2024-12-31"))
                .andExpect(status().isOk());

        verify(transferService).getTransactionHistory(eq(1L), anyInt(), anyInt(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetSummary_Success() throws Exception {
        // Arrange
        TransferService.TransferSummary summary = TransferService.TransferSummary.builder()
                .totalSent(BigDecimal.valueOf(1000))
                .totalReceived(BigDecimal.valueOf(500))
                .monthlyCount(10L)
                .build();
        when(transferService.getTransferSummary(1L)).thenReturn(summary);

        // Act & Assert
        mockMvc.perform(get("/api/transfers/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSent").value(1000))
                .andExpect(jsonPath("$.totalReceived").value(500))
                .andExpect(jsonPath("$.monthlyCount").value(10));

        verify(transferService).getTransferSummary(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetLedgerEntries_AsRegularUser() throws Exception {
        // Arrange
        testUser.setRole(Role.USER);
        testTransfer.setSenderId(1L);
        testTransfer.setReceiverId(2L);

        LedgerEntry ledgerEntry = LedgerEntry.builder()
                .id(1L)
                .transferId(1L)
                .userId(1L)
                .type(LedgerType.DEBIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        when(userService.getUserEntityById(1L)).thenReturn(testUser);
        when(transferService.getTransferById(1L)).thenReturn(testTransfer);
        when(transferService.getLedgerEntriesByTransferId(1L)).thenReturn(Arrays.asList(ledgerEntry));

        // Act & Assert
        mockMvc.perform(get("/api/transfers/1/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].type").value("DEBIT"));

        verify(transferService).getLedgerEntriesByTransferId(1L);
    }

    @Test
    @WithMockUser(username = "100", roles = "ADMIN")
    void testGetLedgerEntries_AsAdmin() throws Exception {
        // Arrange
        User adminUser = new User();
        adminUser.setId(100L);
        adminUser.setRole(Role.ADMIN);

        testTransfer.setSenderId(1L);
        testTransfer.setReceiverId(2L);

        LedgerEntry senderEntry = LedgerEntry.builder()
                .id(1L)
                .transferId(1L)
                .userId(1L)
                .type(LedgerType.DEBIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        LedgerEntry receiverEntry = LedgerEntry.builder()
                .id(2L)
                .transferId(1L)
                .userId(2L)
                .type(LedgerType.CREDIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        when(userService.getUserEntityById(100L)).thenReturn(adminUser);
        when(transferService.getTransferById(1L)).thenReturn(testTransfer);
        when(transferService.getLedgerEntriesByTransferId(1L))
                .thenReturn(Arrays.asList(senderEntry, receiverEntry));

        // Act & Assert
        mockMvc.perform(get("/api/transfers/1/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(transferService).getLedgerEntriesByTransferId(1L);
    }

    @Test
    @WithMockUser(username = "3", roles = "USER")
    void testGetLedgerEntries_AccessDenied() throws Exception {
        // Arrange
        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setRole(Role.USER);

        testTransfer.setSenderId(1L);
        testTransfer.setReceiverId(2L);

        when(userService.getUserEntityById(3L)).thenReturn(otherUser);
        when(transferService.getTransferById(1L)).thenReturn(testTransfer);

        // Act & Assert
        mockMvc.perform(get("/api/transfers/1/ledger"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testGetLedgerEntries_FiltersByUserId() throws Exception {
        // Arrange
        testUser.setRole(Role.USER);
        testTransfer.setSenderId(1L);
        testTransfer.setReceiverId(2L);

        LedgerEntry senderEntry = LedgerEntry.builder()
                .id(1L)
                .transferId(1L)
                .userId(1L)
                .type(LedgerType.DEBIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        LedgerEntry receiverEntry = LedgerEntry.builder()
                .id(2L)
                .transferId(1L)
                .userId(2L)
                .type(LedgerType.CREDIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        when(userService.getUserEntityById(1L)).thenReturn(testUser);
        when(transferService.getTransferById(1L)).thenReturn(testTransfer);
        when(transferService.getLedgerEntriesByTransferId(1L))
                .thenReturn(Arrays.asList(senderEntry, receiverEntry));

        // Act & Assert
        mockMvc.perform(get("/api/transfers/1/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(1));

        verify(transferService).getLedgerEntriesByTransferId(1L);
    }
}
