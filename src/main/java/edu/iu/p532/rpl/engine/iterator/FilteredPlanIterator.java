package edu.iu.p532.rpl.engine.iterator;

import edu.iu.p532.rpl.engine.composite.PlanNode;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class FilteredPlanIterator implements Iterator<PlanNode> {

    private final Iterator<PlanNode> delegate;
    private final Predicate<PlanNode> predicate;
    private PlanNode next;

    public FilteredPlanIterator(PlanNode root, Predicate<PlanNode> predicate) {
        this.delegate = new DepthFirstPlanIterator(root);
        this.predicate = predicate;
        advance();
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public PlanNode next() {
        if (next == null) throw new NoSuchElementException();
        PlanNode result = next;
        advance();
        return result;
    }

    private void advance() {
        next = null;
        while (delegate.hasNext()) {
            PlanNode candidate = delegate.next();
            if (predicate.test(candidate)) {
                next = candidate;
                break;
            }
        }
    }
}
