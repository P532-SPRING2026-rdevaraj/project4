package edu.iu.p532.rpl.dto;

import edu.iu.p532.rpl.domain.ResourceKind;
import java.math.BigDecimal;

public record CreateResourceTypeRequest(
        String name,
        ResourceKind kind,
        String unit,
        BigDecimal initialPoolBalance) {}
