package edu.iu.p532.rpl.engine.visitor;

import edu.iu.p532.rpl.domain.Plan;
import edu.iu.p532.rpl.domain.ProposedAction;
import edu.iu.p532.rpl.domain.ResourceAllocation;
import edu.iu.p532.rpl.engine.composite.PlanNodeVisitor;

import java.math.BigDecimal;

public class ResourceCostVisitor implements PlanNodeVisitor {

    private BigDecimal totalCost = BigDecimal.ZERO;

    @Override
    public void visit(ProposedAction action) {
        for (ResourceAllocation a : action.getAllocations()) {
            if (a.getQuantity() == null) continue;
            BigDecimal unitCost = (a.getResourceType() != null && a.getResourceType().getUnitCost() != null)
                    ? a.getResourceType().getUnitCost()
                    : BigDecimal.ONE;
            totalCost = totalCost.add(a.getQuantity().multiply(unitCost));
        }
    }

    @Override
    public void visit(Plan plan) {
        // composite — children visited recursively
    }

    public BigDecimal getTotalCost() { return totalCost; }
}
