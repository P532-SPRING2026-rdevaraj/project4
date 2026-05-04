package edu.iu.p532.rpl.engine.visitor;

import edu.iu.p532.rpl.domain.ActionStatus;
import edu.iu.p532.rpl.domain.Plan;
import edu.iu.p532.rpl.domain.ProposedAction;
import edu.iu.p532.rpl.engine.composite.PlanNodeVisitor;

public class RiskScoreVisitor implements PlanNodeVisitor {

    private int score = 0;

    @Override
    public void visitLeaf(ProposedAction leaf) {
        if (leaf.getStatus() == ActionStatus.SUSPENDED
                || leaf.getStatus() == ActionStatus.ABANDONED) {
            score++;
        }
    }

    @Override
    public void visitComposite(Plan plan) {
        // composite — children visited recursively
    }

    public int getScore() { return score; }
}
