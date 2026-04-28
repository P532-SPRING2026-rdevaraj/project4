package edu.iu.p532.rpl.dto;

import java.util.List;

public record CreateProtocolRequest(
        String name,
        String description,
        List<Step> steps) {

    public record Step(String stepName, Long subProtocolId, List<String> dependsOn) {}
}
