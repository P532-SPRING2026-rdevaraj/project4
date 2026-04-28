package edu.iu.p532.rpl.dto;

import edu.iu.p532.rpl.domain.ActionStatus;
import java.time.Instant;

public record ActionView(
        Long id,
        String name,
        ActionStatus status,
        String party,
        String location,
        Instant timeRef,
        ActionView.Implemented implemented) {

    public record Implemented(Long id, Instant actualStart, String actualParty, String actualLocation) {}
}
