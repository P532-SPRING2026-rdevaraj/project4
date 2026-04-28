package edu.iu.p532.rpl.dto;

import java.math.BigDecimal;
import java.util.Map;

public record PlanReportRow(
        Long nodeId,
        String name,
        String type,
        String status,
        Map<String, BigDecimal> totals) {}
