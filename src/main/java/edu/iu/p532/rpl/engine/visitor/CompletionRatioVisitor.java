package edu.iu.p532.rpl.engine.visitor;

import edu.iu.p532.rpl.domain.ActionStatus;
import edu.iu.p532.rpl.domain.Plan;
import edu.iu.p532.rpl.domain.ProposedAction;
import edu.iu.p532.rpl.engine.composite.PlanNodeVisitor;

public class CompletionRatioVisitor implements PlanNodeVisitor {

    private int totalLeaves = 0;
    private int completedLeaves = 0;

    @Override
    public void visitLeaf(ProposedAction leaf) {
        totalLeaves++;
        if (leaf.getStatus() == ActionStatus.COMPLETED) completedLeaves++;
    }

    @Override
    public void visitComposite(Plan plan) {
        // composite node — children are visited recursively via accept
    }

    public double getRatio() {
        return totalLeaves == 0 ? 0.0 : (double) completedLeaves / totalLeaves;
    }

    public int getTotalLeaves() { return totalLeaves; }
    public int getCompletedLeaves() { return completedLeaves; }
}
