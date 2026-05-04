package edu.iu.p532.rpl.engine.composite;

import edu.iu.p532.rpl.domain.Plan;
import edu.iu.p532.rpl.domain.ProposedAction;

/**
 * Visitor used to traverse the Composite plan tree without leaking
 * conditionals into client code. Week-2 metric visitors will implement this
 * interface; existing node classes will not need to change.
 */
public interface PlanNodeVisitor {
    void visitLeaf(ProposedAction leaf);
    void visitComposite(Plan plan);
}
