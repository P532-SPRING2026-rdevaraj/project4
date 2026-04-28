package edu.iu.p532.rpl.dto;

import java.util.List;

public record PlanReport(Long planId, String planName, List<PlanReportRow> rows) {}
