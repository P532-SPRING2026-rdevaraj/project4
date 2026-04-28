package edu.iu.p532.rpl.dto;

import edu.iu.p532.rpl.domain.ActionStatus;
import java.util.List;

public record PlanTreeView(
        Long id,
        String name,
        String type,
        ActionStatus status,
        List<PlanTreeView> children,
        List<ActionView> leafDetail) {}
