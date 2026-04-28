package edu.iu.p532.rpl.engine.composite;

import edu.iu.p532.rpl.domain.ActionStatus;
import edu.iu.p532.rpl.domain.ResourceType;
import java.math.BigDecimal;

/**
 * Composite-pattern interface for plan nodes.
 * Both leaf {@code ProposedAction} and composite {@code Plan} implement this so
 * iterator/visitor/report code never needs to type-check.
 */
public interface PlanNode {
    Long getId();
    String getName();
    ActionStatus getStatus();
    BigDecimal getTotalAllocatedQuantity(ResourceType resourceType);
    boolean isLeaf();
    void accept(PlanNodeVisitor visitor);
}
