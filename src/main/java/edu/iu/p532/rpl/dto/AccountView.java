package edu.iu.p532.rpl.dto;

import edu.iu.p532.rpl.domain.AccountKind;
import java.math.BigDecimal;

public record AccountView(
        Long id,
        String name,
        AccountKind kind,
        String resourceTypeName,
        String unit,
        BigDecimal balance) {}
