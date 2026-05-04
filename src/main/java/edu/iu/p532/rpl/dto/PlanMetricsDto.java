package edu.iu.p532.rpl.dto;

import java.math.BigDecimal;

public record PlanMetricsDto(
        Long nodeId,
        double completionRatio,
        int totalLeaves,
        int completedLeaves,
        BigDecimal totalResourceCost,
        int riskScore
) {}
