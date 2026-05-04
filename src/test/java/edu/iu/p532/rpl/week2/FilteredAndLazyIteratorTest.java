package edu.iu.p532.rpl.week2;

import edu.iu.p532.rpl.domain.ActionStatus;
import edu.iu.p532.rpl.domain.Plan;
import edu.iu.p532.rpl.domain.ProposedAction;
import edu.iu.p532.rpl.engine.composite.PlanNode;
import edu.iu.p532.rpl.engine.iterator.FilteredPlanIterator;
import edu.iu.p532.rpl.engine.iterator.LazySubtreeIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilteredAndLazyIteratorTest {

    private Plan root;
    private Plan child;
    private ProposedAction proposed;
    private ProposedAction completed;

    @BeforeEach
    void setUp() {
        // root → child (sub-plan) → proposed action
        //       → completed action
        proposed = new ProposedAction();
        proposed.setId(1L);
        proposed.setName("proposed-action");
        proposed.setStatus(ActionStatus.PROPOSED);
        proposed.setAllocations(new ArrayList<>());

        completed = new ProposedAction();
        completed.setId(2L);
        completed.setName("completed-action");
        completed.setStatus(ActionStatus.COMPLETED);
        completed.setAllocations(new ArrayList<>());

        child = new Plan();
        child.setId(10L);
        child.setName("child-plan");
        child.setChildPlans(new ArrayList<>());
        child.setChildActions(new ArrayList<>(List.of(proposed)));

        root = new Plan();
        root.setId(100L);
        root.setName("root-plan");
        root.setChildPlans(new ArrayList<>(List.of(child)));
        root.setChildActions(new ArrayList<>(List.of(completed)));
    }

    // --- FilteredPlanIterator ---

    @Test
    void filtered_onlyProposed_returnsProposedNodes() {
        // Arrange
        FilteredPlanIterator it = new FilteredPlanIterator(root,
                n -> n.getStatus() == ActionStatus.PROPOSED);
        // Act
        List<PlanNode> result = collect(it);
        // Assert: only the 'proposed' action passes; child plan is PROPOSED too (empty=PROPOSED? no - child has children)
        assertThat(result).anyMatch(n -> n.getId().equals(1L));
        result.forEach(n -> assertThat(n.getStatus()).isEqualTo(ActionStatus.PROPOSED));
    }

    @Test
    void filtered_onlyCompleted_returnsCompletedAction() {
        FilteredPlanIterator it = new FilteredPlanIterator(root,
                n -> n.getStatus() == ActionStatus.COMPLETED);
        List<PlanNode> result = collect(it);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void filtered_neverMatches_returnsEmpty() {
        FilteredPlanIterator it = new FilteredPlanIterator(root,
                n -> n.getStatus() == ActionStatus.ABANDONED);
        assertThat(collect(it)).isEmpty();
    }

    @Test
    void filtered_hasNext_falseWhenEmpty() {
        FilteredPlanIterator it = new FilteredPlanIterator(root, n -> false);
        assertThat(it.hasNext()).isFalse();
        assertThatThrownBy(it::next)
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    // --- LazySubtreeIterator ---

    @Test
    void lazy_depthZero_returnsOnlyRoot() {
        // Arrange
        LazySubtreeIterator it = new LazySubtreeIterator(root, 0);
        // Act
        List<PlanNode> result = collect(it);
        // Assert: depth limit 0 means don't descend into children
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }

    @Test
    void lazy_depthOne_returnsRootAndDirectChildren() {
        LazySubtreeIterator it = new LazySubtreeIterator(root, 1);
        List<PlanNode> result = collect(it);
        // root + child (sub-plan) + completed (leaf action at depth 1)
        assertThat(result).hasSize(3);
    }

    @Test
    void lazy_depthMax_returnsAllNodes() {
        LazySubtreeIterator it = new LazySubtreeIterator(root, Integer.MAX_VALUE);
        List<PlanNode> result = collect(it);
        // root, child, completed, proposed = 4 nodes
        assertThat(result).hasSize(4);
    }

    @Test
    void lazy_nullRoot_returnsEmpty() {
        LazySubtreeIterator it = new LazySubtreeIterator(null, 5);
        assertThat(it.hasNext()).isFalse();
    }

    private List<PlanNode> collect(java.util.Iterator<PlanNode> it) {
        List<PlanNode> out = new ArrayList<>();
        while (it.hasNext()) out.add(it.next());
        return out;
    }
}
