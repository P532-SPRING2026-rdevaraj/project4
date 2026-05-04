package edu.iu.p532.rpl.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record EntryView(
        Long id,
        Long accountId,
        String accountName,
        BigDecimal amount,
        Instant chargedAt,
        Instant bookedAt,
        Long originatingActionId,
        String originatingActionName) {}
