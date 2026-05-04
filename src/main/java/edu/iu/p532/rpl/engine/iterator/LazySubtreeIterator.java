package edu.iu.p532.rpl.engine.iterator;

import edu.iu.p532.rpl.domain.Plan;
import edu.iu.p532.rpl.engine.composite.PlanNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class LazySubtreeIterator implements Iterator<PlanNode> {

    private final Deque<int[]> depthStack = new ArrayDeque<>();
    private final Deque<PlanNode> nodeStack = new ArrayDeque<>();
    private final int depthLimit;

    public LazySubtreeIterator(PlanNode root, int depthLimit) {
        this.depthLimit = depthLimit;
        if (root != null) {
            nodeStack.push(root);
            depthStack.push(new int[]{0});
        }
    }

    @Override
    public boolean hasNext() {
        return !nodeStack.isEmpty();
    }

    @Override
    public PlanNode next() {
        if (nodeStack.isEmpty()) throw new NoSuchElementException();
        PlanNode current = nodeStack.pop();
        int depth = depthStack.pop()[0];

        if (current instanceof Plan plan && depth < depthLimit) {
            java.util.List<PlanNode> children = plan.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                nodeStack.push(children.get(i));
                depthStack.push(new int[]{depth + 1});
            }
        }
        return current;
    }
}
