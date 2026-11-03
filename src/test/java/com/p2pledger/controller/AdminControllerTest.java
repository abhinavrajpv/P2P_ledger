
package com.p2pledger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2pledger.model.*;
import com.p2pledger.service.AdminService;
import com.p2pledger.service.ScheduledJobService;
import com.p2pledger.service.TransferService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(AdminControllerTest.MethodSecurityTestConfig.class)
class AdminControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private TransferService transferService;

    @MockBean
    private ScheduledJobService scheduledJobService;

    private AdminService.DashboardData dashboardData;
    private Transfer testTransfer;
    private FraudAlert testFraudAlert;

    @BeforeEach
    void setUp() {
        dashboardData = new AdminService.DashboardData(
                BigDecimal.valueOf(10000),
                50L,
                45L,
                10L,
                5L,
                BigDecimal.valueOf(1000),
                20L,
                18L,
                2L,
                BigDecimal.valueOf(500)
        );

        testTransfer = Transfer.builder()
                .id(1L)
                .senderId(1L)
                .receiverId(2L)
                .amount(BigDecimal.valueOf(500))
                .status(TransferStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        testFraudAlert = FraudAlert.builder()
                .id(1L)
                .transferId(1L)
                .reason("High amount")
                .status(FraudStatus.OPEN)
                .build();
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetDashboard_Success() throws Exception {
        when(adminService.getDashboardData()).thenReturn(dashboardData);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volume30Day").value(10000))
                .andExpect(jsonPath("$.usersCount").value(50))
                .andExpect(jsonPath("$.walletsCount").value(45))
                .andExpect(jsonPath("$.pendingSettlements").value(10))
                .andExpect(jsonPath("$.flaggedTransactions").value(5));

        verify(adminService).getDashboardData();
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetDailyVolumeChart_Success() throws Exception {
        List<AdminService.DailyVolumeData> chartData = Arrays.asList(
                new AdminService.DailyVolumeData(
                        LocalDate.now(),
                        BigDecimal.valueOf(1000)
                )
        );

        when(adminService.getDailyVolumeChart()).thenReturn(chartData);

        mockMvc.perform(get("/api/admin/dashboard/chart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].volume").value(1000));

        verify(adminService).getDailyVolumeChart();
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetAllUsers_Success() throws Exception {
        AdminService.UserWithWallet userWithWallet =
                new AdminService.UserWithWallet(
                        1L,
                        "Test User",
                        "test@example.com",
                        "1234567890",
                        Role.USER,
                        true,
                        LocalDateTime.now(),
                        null,
                        BigDecimal.valueOf(1000),
                        WalletStatus.ACTIVE
                );

        Page<AdminService.UserWithWallet> page =
                new PageImpl<>(Arrays.asList(userWithWallet));

        when(adminService.getAllUsersWithWallet(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test User"))
                .andExpect(jsonPath("$.content[0].walletBalance").value(1000));

        verify(adminService).getAllUsersWithWallet(any());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetUserDetail_Success() throws Exception {
        AdminService.UserDetail userDetail =
                new AdminService.UserDetail(
                        1L,
                        "Test User",
                        "test@example.com",
                        "1234567890",
                        Role.USER,
                        true,
                        LocalDateTime.now(),
                        null,
                        BigDecimal.valueOf(1000),
                        WalletStatus.ACTIVE,
                        Arrays.asList()
                );

        when(adminService.getUserDetail(1L)).thenReturn(userDetail);

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test User"));

        verify(adminService).getUserDetail(1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testFreezeUserWallet_Success() throws Exception {
        doNothing().when(adminService).freezeUserWallet(1L, 1L);

        mockMvc.perform(post("/api/admin/users/1/freeze")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("User wallet frozen successfully"));

        verify(adminService).freezeUserWallet(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testUnfreezeUserWallet_Success() throws Exception {
        doNothing().when(adminService).unfreezeUserWallet(1L, 1L);

        mockMvc.perform(post("/api/admin/users/1/unfreeze")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("User wallet unfrozen successfully"));

        verify(adminService).unfreezeUserWallet(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetSettlementSummary_Success() throws Exception {
        AdminService.SettlementSummary summary =
                new AdminService.SettlementSummary(5L, 3L, 2L, 10L);

        when(adminService.getSettlementSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/admin/settlements/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueToday").value(5))
                .andExpect(jsonPath("$.dueTomorrow").value(3))
                .andExpect(jsonPath("$.overdue").value(2))
                .andExpect(jsonPath("$.totalPending").value(10));

        verify(adminService).getSettlementSummary();
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetTransactions_WithFilters() throws Exception {
        Page<Transfer> page =
                new PageImpl<>(Arrays.asList(testTransfer));

        when(adminService.getTransactionsWithFilters(
                any(), anyString(), anyLong(), anyLong(),
                any(), any(), any(), any()
        )).thenReturn(page);

        mockMvc.perform(get("/api/admin/transactions")
                        .param("status", "PENDING")
                        .param("senderId", "1")
                        .param("receiverId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(500));

        verify(adminService).getTransactionsWithFilters(
                any(), eq("PENDING"), eq(1L), eq(2L),
                any(), any(), any(), any()
        );
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetTransactions_WithDateRange() throws Exception {
        Page<Transfer> page =
                new PageImpl<>(Arrays.asList(testTransfer));

        when(adminService.getTransactionsWithFilters(
                any(), any(), any(), any(),
                any(), any(), any(), any()
        )).thenReturn(page);

        mockMvc.perform(get("/api/admin/transactions")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk());

        verify(adminService).getTransactionsWithFilters(
                any(), any(), any(), any(),
                any(), any(), any(), any()
        );
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetTransactions_WithAmountRange() throws Exception {
        Page<Transfer> page =
                new PageImpl<>(Arrays.asList(testTransfer));

        when(adminService.getTransactionsWithFilters(
                any(), any(), any(), any(),
                any(), any(), any(), any()
        )).thenReturn(page);

        mockMvc.perform(get("/api/admin/transactions")
                        .param("minAmount", "100")
                        .param("maxAmount", "1000"))
                .andExpect(status().isOk());

        verify(adminService).getTransactionsWithFilters(
                any(), any(), any(), any(),
                any(), any(), any(), any()
        );
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetPendingSettlements_Success() throws Exception {
        Page<Transfer> page =
                new PageImpl<>(Arrays.asList(testTransfer));

        when(adminService.getPendingSettlements(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        verify(adminService).getPendingSettlements(any());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testProcessSettlement_Success() throws Exception {
        AdminController.ProcessSettlementRequest request =
                new AdminController.ProcessSettlementRequest();

        request.setTransferId(1L);

        testTransfer.setStatus(TransferStatus.SUCCESS);

        when(adminService.processSettlement(1L, 1L))
                .thenReturn(testTransfer);

        mockMvc.perform(post("/api/admin/settlements/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(adminService).processSettlement(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetFlaggedTransactions_Success() throws Exception {
        Page<FraudAlert> page =
                new PageImpl<>(Arrays.asList(testFraudAlert));

        when(adminService.getFlaggedTransactions(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/fraud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reason").value("High amount"));

        verify(adminService).getFlaggedTransactions(any());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testApproveTransaction_Success() throws Exception {
        testTransfer.setAdminReviewStatus(AdminReviewStatus.APPROVED);

        when(adminService.approveTransaction(1L, 1L))
                .thenReturn(testTransfer);

        mockMvc.perform(post("/api/admin/fraud/1/approve")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminReviewStatus").value("APPROVED"));

        verify(adminService).approveTransaction(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testRefundTransaction_Success() throws Exception {
        testTransfer.setStatus(TransferStatus.DECLINED);
        testTransfer.setAdminReviewStatus(AdminReviewStatus.REFUNDED);

        when(adminService.refundTransaction(1L, 1L))
                .thenReturn(testTransfer);

        mockMvc.perform(post("/api/admin/fraud/1/refund")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.adminReviewStatus").value("REFUNDED"));

        verify(adminService).refundTransaction(1L, 1L);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetJobExecutionHistory_WithJobName() throws Exception {
        List<ScheduledJobLog> logs = Arrays.asList(
                ScheduledJobLog.builder()
                        .jobName("test_job")
                        .build()
        );

        when(scheduledJobService.getJobExecutionHistory("test_job"))
                .thenReturn(logs);

        mockMvc.perform(get("/api/admin/jobs")
                        .param("jobName", "test_job"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobName").value("test_job"));

        verify(scheduledJobService).getJobExecutionHistory("test_job");
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetJobExecutionHistory_WithoutJobName() throws Exception {
        List<ScheduledJobLog> logs = Arrays.asList(
                ScheduledJobLog.builder().jobName("job1").build(),
                ScheduledJobLog.builder().jobName("job2").build()
        );

        when(scheduledJobService.getJobExecutionHistory(null))
                .thenReturn(logs);

        mockMvc.perform(get("/api/admin/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(scheduledJobService).getJobExecutionHistory(null);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetReconciliationReport_Success() throws Exception {
        List<DailySummary> summaries = Arrays.asList(
                DailySummary.builder()
                        .summaryDate(LocalDate.now())
                        .build()
        );

        when(adminService.getReconciliationReport(any(), any()))
                .thenReturn(summaries);

        mockMvc.perform(get("/api/admin/reconciliation")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(adminService).getReconciliationReport(any(), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetLoginAttempts_WithFilters() throws Exception {
        List<LoginAttempts> attempts = Arrays.asList(
                LoginAttempts.builder()
                        .userId(1L)
                        .successful(true)
                        .build()
        );

        when(adminService.getLoginAttempts(
                1L, "test@example.com", true
        )).thenReturn(attempts);

        mockMvc.perform(get("/api/admin/login-attempts")
                        .param("userId", "1")
                        .param("identifier", "test@example.com")
                        .param("successful", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].successful").value(true));

        verify(adminService).getLoginAttempts(
                1L, "test@example.com", true
        );
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetLoginAttempts_NoFilters() throws Exception {
        List<LoginAttempts> attempts = Arrays.asList(
                LoginAttempts.builder().build()
        );

        when(adminService.getLoginAttempts(null, null, null))
                .thenReturn(attempts);

        mockMvc.perform(get("/api/admin/login-attempts"))
                .andExpect(status().isOk());

        verify(adminService).getLoginAttempts(null, null, null);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void testGetFailedLoginAttempts_Success() throws Exception {
        List<LoginAttempts> attempts = Arrays.asList(
                LoginAttempts.builder()
                        .successful(false)
                        .build()
        );

        when(adminService.getFailedLoginAttempts()).thenReturn(attempts);

        mockMvc.perform(get("/api/admin/login-attempts/failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].successful").value(false));

        verify(adminService).getFailedLoginAttempts();
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void testDashboard_ForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Access Denied"));
    }
}
