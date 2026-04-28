package edu.iu.p532.rpl.dto;

import edu.iu.p532.rpl.domain.AllocationKind;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateAllocationRequest(
        Long resourceTypeId,
        BigDecimal quantity,
        AllocationKind kind,
        String assetId,
        Instant periodStart,
        Instant periodEnd) {}
