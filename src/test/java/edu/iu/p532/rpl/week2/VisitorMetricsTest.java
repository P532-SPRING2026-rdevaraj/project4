package edu.iu.p532.rpl.week2;

import edu.iu.p532.rpl.domain.*;
import edu.iu.p532.rpl.engine.visitor.CompletionRatioVisitor;
import edu.iu.p532.rpl.engine.visitor.ResourceCostVisitor;
import edu.iu.p532.rpl.engine.visitor.RiskScoreVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VisitorMetricsTest {

    private Plan root;
    private ProposedAction proposedAction;
    private ProposedAction completedAction;
    private ProposedAction suspendedAction;

    @BeforeEach
    void setUp() {
        proposedAction = new ProposedAction();
        proposedAction.setId(1L);
        proposedAction.setStatus(ActionStatus.PROPOSED);
        proposedAction.setAllocations(new ArrayList<>());

        completedAction = new ProposedAction();
        completedAction.setId(2L);
        completedAction.setStatus(ActionStatus.COMPLETED);
        completedAction.setAllocations(new ArrayList<>());

        suspendedAction = new ProposedAction();
        suspendedAction.setId(3L);
        suspendedAction.setStatus(ActionStatus.SUSPENDED);
        suspendedAction.setAllocations(new ArrayList<>());

        root = new Plan();
        root.setId(100L);
        root.setChildPlans(new ArrayList<>());
        root.setChildActions(new ArrayList<>(List.of(proposedAction, completedAction, suspendedAction)));
    }

    // --- CompletionRatioVisitor ---

    @Test
    void completionRatio_oneOfThreeCompleted_returnsCorrectRatio() {
        CompletionRatioVisitor v = new CompletionRatioVisitor();
        root.accept(v);
        assertThat(v.getTotalLeaves()).isEqualTo(3);
        assertThat(v.getCompletedLeaves()).isEqualTo(1);
        assertThat(v.getRatio()).isCloseTo(1.0 / 3.0, within(0.001));
    }

    @Test
    void completionRatio_noLeaves_returnsZero() {
        Plan empty = new Plan();
        empty.setId(99L);
        empty.setChildPlans(new ArrayList<>());
        empty.setChildActions(new ArrayList<>());
        CompletionRatioVisitor v = new CompletionRatioVisitor();
        empty.accept(v);
        assertThat(v.getRatio()).isEqualTo(0.0);
    }

    // --- ResourceCostVisitor ---

    @Test
    void resourceCost_withUnitCost_sumsCorrently() {
        ResourceType rt = new ResourceType();
        rt.setUnitCost(BigDecimal.valueOf(10));
        rt.setKind(ResourceKind.CONSUMABLE);

        ResourceAllocation alloc = new ResourceAllocation();
        alloc.setQuantity(BigDecimal.valueOf(3));
        alloc.setResourceType(rt);
        alloc.setKind(AllocationKind.GENERAL);
        completedAction.getAllocations().add(alloc);

        ResourceCostVisitor v = new ResourceCostVisitor();
        root.accept(v);
        assertThat(v.getTotalCost()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    void resourceCost_noUnitCost_defaultsToOne() {
        ResourceType rt = new ResourceType();
        rt.setKind(ResourceKind.CONSUMABLE);
        // unitCost is null → defaults to 1

        ResourceAllocation alloc = new ResourceAllocation();
        alloc.setQuantity(BigDecimal.valueOf(5));
        alloc.setResourceType(rt);
        alloc.setKind(AllocationKind.GENERAL);
        proposedAction.getAllocations().add(alloc);

        ResourceCostVisitor v = new ResourceCostVisitor();
        proposedAction.accept(v);
        assertThat(v.getTotalCost()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    // --- RiskScoreVisitor ---

    @Test
    void riskScore_suspendedAndAbandoned_countsBoth() {
        suspendedAction.setStatus(ActionStatus.SUSPENDED);
        ProposedAction abandoned = new ProposedAction();
        abandoned.setId(4L);
        abandoned.setStatus(ActionStatus.ABANDONED);
        abandoned.setAllocations(new ArrayList<>());
        root.getChildActions().add(abandoned);

        RiskScoreVisitor v = new RiskScoreVisitor();
        root.accept(v);
        assertThat(v.getScore()).isEqualTo(2); // suspended + abandoned
    }

    @Test
    void riskScore_allCompleted_returnsZero() {
        completedAction.setStatus(ActionStatus.COMPLETED);
        proposedAction.setStatus(ActionStatus.COMPLETED);
        suspendedAction.setStatus(ActionStatus.COMPLETED);

        RiskScoreVisitor v = new RiskScoreVisitor();
        root.accept(v);
        assertThat(v.getScore()).isEqualTo(0);
    }

    @Test
    void riskScore_leafNode_scoresDirectly() {
        suspendedAction.setStatus(ActionStatus.SUSPENDED);
        RiskScoreVisitor v = new RiskScoreVisitor();
        suspendedAction.accept(v);
        assertThat(v.getScore()).isEqualTo(1);
    }
}
