package com.circleguard.dashboard.integration;

import com.circleguard.dashboard.controller.AnalyticsController;
import com.circleguard.dashboard.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@ActiveProfiles("test")
class AnalyticsKAnonymityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void shouldReturnHealthBoardStatsFromService() throws Exception {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 250L);
        stats.put("greenCount", 220L);
        stats.put("redCount", 30L);

        when(analyticsService.getGlobalHealthStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/analytics/health-board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(250))
                .andExpect(jsonPath("$.greenCount").value(220));
    }

    @Test
    void shouldReturnDepartmentStatsForGivenDepartment() throws Exception {
        Map<String, Object> filtered = new LinkedHashMap<>();
        filtered.put("totalUsers", 50L);
        filtered.put("greenCount", 45L);
        filtered.put("redCount", "<5");
        filtered.put("department", "Engineering");

        when(analyticsService.getDepartmentStats("Engineering")).thenReturn(filtered);

        mockMvc.perform(get("/api/v1/analytics/department/Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.redCount").value("<5"));
    }

    @Test
    void shouldReturnCampusSummary() throws Exception {
        Map<String, Object> summary = Map.of("campus", "main", "activeUsers", 500L);
        when(analyticsService.getCampusSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campus").value("main"));
    }

    @Test
    void shouldReturnEntryTrendsForLocation() throws Exception {
        UUID locationId = UUID.randomUUID();
        List<Map<String, Object>> trends = List.of(
                Map.of("hour", "2024-01-01T10:00:00", "entry_count", 12L),
                Map.of("hour", "2024-01-01T11:00:00", "entry_count", "<5", "note", "Insufficient data for privacy")
        );

        when(analyticsService.getEntryTrends(any(UUID.class))).thenReturn(trends);

        mockMvc.perform(get("/api/v1/analytics/trends/" + locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].entry_count").value("<5"))
                .andExpect(jsonPath("$[1].note").exists());
    }

    @Test
    void shouldReturnTimeSeriesWithDefaultParameters() throws Exception {
        List<Map<String, Object>> series = List.of(
                Map.of("bucket", "2024-01-01T10:00:00", "status", "ACTIVE", "total", 200)
        );

        when(analyticsService.getTimeSeries(anyString(), any(Integer.class))).thenReturn(series);

        mockMvc.perform(get("/api/v1/analytics/time-series"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }
}
