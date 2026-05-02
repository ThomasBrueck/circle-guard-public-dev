package com.circleguard.dashboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KAnonymityFilterTest {

    private KAnonymityFilter filter;

    @BeforeEach
    void setUp() {
        filter = new KAnonymityFilter();
    }

    @Test
    void shouldMaskEntireResultWhenTotalPopulationBelowK() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 3L);
        stats.put("greenCount", 2L);
        stats.put("department", "Engineering");

        Map<String, Object> result = filter.apply(stats);

        assertEquals("<5", result.get("totalUsers"));
        assertNotNull(result.get("note"));
        assertFalse(result.containsKey("greenCount"));
    }

    @Test
    void shouldMaskIndividualCountFieldsBelowK() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 100L);
        stats.put("greenCount", 90L);
        stats.put("redCount", 2L);

        Map<String, Object> result = filter.apply(stats);

        assertEquals(90L, result.get("greenCount"));
        assertEquals("<5", result.get("redCount"));
    }

    @Test
    void shouldNotMaskCountsAtOrAboveK() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 50L);
        stats.put("greenCount", 45L);
        stats.put("yellowCount", 5L);

        Map<String, Object> result = filter.apply(stats);

        assertEquals(45L, result.get("greenCount"));
        assertEquals(5L, result.get("yellowCount"));
    }

    @Test
    void shouldReturnEmptyMapForNullInput() {
        Map<String, Object> result = filter.apply(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRespectCustomKThreshold() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 100L);
        stats.put("redCount", 7L);

        Map<String, Object> resultK5 = filter.apply(stats, 5);
        Map<String, Object> resultK10 = filter.apply(stats, 10);

        assertEquals(7L, resultK5.get("redCount"));
        assertEquals("<10", resultK10.get("redCount"));
    }

    @Test
    void shouldPreserveDepartmentFieldInMaskedResult() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 2L);
        stats.put("department", "Physics");

        Map<String, Object> result = filter.apply(stats);

        assertEquals("Physics", result.get("department"));
    }
}
