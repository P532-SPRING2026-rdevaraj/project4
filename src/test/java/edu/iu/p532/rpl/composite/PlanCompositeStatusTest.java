package edu.iu.p532.rpl.composite;

import edu.iu.p532.rpl.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlanCompositeStatusTest {

    @Test
    void emptyPlan_isProposed() {
        // Arrange
        Plan plan = new Plan();
        // Assert
        assertThat(plan.getStatus()).isEqualTo(ActionStatus.PROPOSED);
    }

    @Test
    void allChildrenCompleted_planIsCompleted() {
        // Arrange
        Plan plan = new Plan();
        plan.getChildActions().add(actionWith(ActionStatus.COMPLETED));
        plan.getChildActions().add(actionWith(ActionStatus.COMPLETED));
        // Assert
        assertThat(plan.getStatus()).isEqualTo(ActionStatus.COMPLETED);
    }

    @Test
    void anyChildInProgress_planIsInProgress() {
        Plan plan = new Plan();
        plan.getChildActions().add(actionWith(ActionStatus.PROPOSED));
        plan.getChildActions().add(actionWith(ActionStatus.IN_PROGRESS));
        assertThat(plan.getStatus()).isEqualTo(ActionStatus.IN_PROGRESS);
    }

    @Test
    void anyChildCompletedNotAll_planIsInProgress() {
        Plan plan = new Plan();
        plan.getChildActions().add(actionWith(ActionStatus.PROPOSED));
        plan.getChildActions().add(actionWith(ActionStatus.COMPLETED));
        assertThat(plan.getStatus()).isEqualTo(ActionStatus.IN_PROGRESS);
    }

    @Test
    void suspendedAndNoneInProgress_planIsSuspended() {
        Plan plan = new Plan();
        plan.getChildActions().add(actionWith(ActionStatus.SUSPENDED));
        plan.getChildActions().add(actionWith(ActionStatus.PROPOSED));
        assertThat(plan.getStatus()).isEqualTo(ActionStatus.SUSPENDED);
    }

    @Test
    void allChildrenAbandoned_planIsAbandoned() {
        Plan plan = new Plan();
        plan.getChildActions().add(actionWith(ActionStatus.ABANDONED));
        plan.getChildActions().add(actionWith(ActionStatus.ABANDONED));
        assertThat(plan.getStatus()).isEqualTo(ActionStatus.ABANDONED);
    }

    @Test
    void totalAllocatedQuantity_sumsAcrossLeaves() {
        // Arrange
        ResourceType rt = new ResourceType();
        rt.setId(1L);
        ProposedAction a1 = new ProposedAction();
        a1.setStatus(ActionStatus.PROPOSED);
        a1.getAllocations().add(allocation(rt, "5"));
        ProposedAction a2 = new ProposedAction();
        a2.setStatus(ActionStatus.PROPOSED);
        a2.getAllocations().add(allocation(rt, "3"));
        Plan plan = new Plan();
        plan.getChildActions().add(a1);
        plan.getChildActions().add(a2);
        // Assert
        assertThat(plan.getTotalAllocatedQuantity(rt)).isEqualByComparingTo("8");
    }

    @Test
    void compositeNestedPlans_aggregateStatusFromGrandchildren() {
        Plan outer = new Plan();
        Plan inner = new Plan();
        inner.getChildActions().add(actionWith(ActionStatus.IN_PROGRESS));
        outer.getChildPlans().add(inner);
        assertThat(outer.getStatus()).isEqualTo(ActionStatus.IN_PROGRESS);
    }

    private static ProposedAction actionWith(ActionStatus s) {
        ProposedAction a = new ProposedAction();
        a.setStatus(s);
        return a;
    }

    private static ResourceAllocation allocation(ResourceType rt, String qty) {
        ResourceAllocation a = new ResourceAllocation();
        a.setResourceType(rt);
        a.setQuantity(new BigDecimal(qty));
        a.setKind(AllocationKind.GENERAL);
        return a;
    }
}
