package edu.iu.p532.rpl.engine.iterator;

import edu.iu.p532.rpl.domain.Plan;
import edu.iu.p532.rpl.engine.composite.PlanNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Pre-order depth-first traversal of an in-memory {@link PlanNode} tree.
 * Operates entirely on already-loaded entities — never touches the database
 * — so it can be unit-tested without Spring or JPA. The Manager is
 * responsible for fetching the subtree before constructing the iterator.
 */
public class DepthFirstPlanIterator implements Iterator<PlanNode> {

    private final Deque<PlanNode> stack = new ArrayDeque<>();

    public DepthFirstPlanIterator(PlanNode root) {
        if (root != null) {
            stack.push(root);
        }
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public PlanNode next() {
        if (stack.isEmpty()) throw new NoSuchElementException();
        PlanNode current = stack.pop();
        if (current instanceof Plan plan) {
            List<PlanNode> children = plan.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return current;
    }
}
