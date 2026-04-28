package edu.iu.p532.rpl.iterator;

import edu.iu.p532.rpl.domain.ActionStatus;
import edu.iu.p532.rpl.domain.Plan;
import edu.iu.p532.rpl.domain.ProposedAction;
import edu.iu.p532.rpl.engine.composite.PlanNode;
import edu.iu.p532.rpl.engine.iterator.DepthFirstPlanIterator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepthFirstPlanIteratorTest {

    @Test
    void traversal_visitsRootBeforeChildren_actionLeafOrder() {
        // Arrange: root plan with two leaf actions
        Plan root = namedPlan("root");
        root.getChildActions().add(namedAction("a1"));
        root.getChildActions().add(namedAction("a2"));

        // Act
        List<String> order = collect(root);

        // Assert
        assertThat(order).containsExactly("root", "a1", "a2");
    }

    @Test
    void traversal_descendsIntoSubPlansBeforeSiblings() {
        // Arrange
        Plan root = namedPlan("root");
        Plan inner = namedPlan("inner");
        inner.getChildActions().add(namedAction("inner-a"));
        root.getChildPlans().add(inner);
        root.getChildActions().add(namedAction("root-a"));

        // Act
        List<String> order = collect(root);

        // Assert: depth-first, child plans first per Plan#getChildren ordering
        assertThat(order).containsExactly("root", "inner", "inner-a", "root-a");
    }

    @Test
    void hasNext_isFalseWhenExhausted() {
        Plan root = namedPlan("root");
        Iterator<PlanNode> it = new DepthFirstPlanIterator(root);
        assertThat(it.hasNext()).isTrue();
        it.next();
        assertThat(it.hasNext()).isFalse();
    }

    @Test
    void next_throwsNoSuchElementWhenExhausted() {
        Iterator<PlanNode> it = new DepthFirstPlanIterator(null);
        assertThatThrownBy(it::next).isInstanceOf(NoSuchElementException.class);
    }

    private static Plan namedPlan(String name) {
        Plan p = new Plan();
        p.setName(name);
        return p;
    }

    private static ProposedAction namedAction(String name) {
        ProposedAction a = new ProposedAction();
        a.setName(name);
        a.setStatus(ActionStatus.PROPOSED);
        return a;
    }

    private static List<String> collect(Plan root) {
        List<String> names = new ArrayList<>();
        Iterator<PlanNode> it = new DepthFirstPlanIterator(root);
        while (it.hasNext()) names.add(it.next().getName());
        return names;
    }
}
