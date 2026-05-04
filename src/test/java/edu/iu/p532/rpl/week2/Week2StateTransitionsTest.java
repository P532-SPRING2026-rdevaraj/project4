package edu.iu.p532.rpl.week2;

import edu.iu.p532.rpl.domain.ActionStatus;
import edu.iu.p532.rpl.domain.ImplementedAction;
import edu.iu.p532.rpl.domain.ProposedAction;
import edu.iu.p532.rpl.engine.state.*;
import edu.iu.p532.rpl.exception.IllegalStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Week2StateTransitionsTest {

    private ProposedState proposed;
    private PendingApprovalState pendingApproval;
    private ReopenedState reopened;
    private CompletedState completed;

    private ProposedAction action;
    private ActionStateCallbacks callbacks;
    private ActionContext ctx;

    @BeforeEach
    void setUp() {
        proposed = new ProposedState();
        pendingApproval = new PendingApprovalState();
        reopened = new ReopenedState();
        completed = new CompletedState();

        action = new ProposedAction();
        action.setStatus(ActionStatus.PROPOSED);
        callbacks = mock(ActionStateCallbacks.class);
        ctx = new ActionContext(action, callbacks);
    }

    // --- ProposedState Week 2 ---

    @Test
    void implement_fromProposed_throwsIllegalTransition() {
        assertThatThrownBy(() -> proposed.implement(ctx))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void submitForApproval_fromProposed_movesToPendingApproval() {
        proposed.submitForApproval(ctx);
        assertThat(action.getStatus()).isEqualTo(ActionStatus.PENDING_APPROVAL);
    }

    // --- PendingApprovalState ---

    @Test
    void approve_fromPendingApproval_movesToInProgress() {
        action.setStatus(ActionStatus.PENDING_APPROVAL);
        ImplementedAction impl = new ImplementedAction();
        when(callbacks.onImplement(action)).thenReturn(impl);
        pendingApproval.approve(ctx);
        assertThat(action.getStatus()).isEqualTo(ActionStatus.IN_PROGRESS);
        verify(callbacks).onImplement(action);
    }

    @Test
    void reject_fromPendingApproval_movesBackToProposed() {
        action.setStatus(ActionStatus.PENDING_APPROVAL);
        pendingApproval.reject(ctx);
        assertThat(action.getStatus()).isEqualTo(ActionStatus.PROPOSED);
    }

    @Test
    void complete_fromPendingApproval_throwsIllegalTransition() {
        action.setStatus(ActionStatus.PENDING_APPROVAL);
        assertThatThrownBy(() -> pendingApproval.complete(ctx))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void suspend_fromPendingApproval_throwsIllegalTransition() {
        action.setStatus(ActionStatus.PENDING_APPROVAL);
        assertThatThrownBy(() -> pendingApproval.suspend(ctx, "reason"))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    // --- CompletedState reopen ---

    @Test
    void reopen_fromCompleted_movesToReopened() {
        ImplementedAction impl = new ImplementedAction();
        impl.setStatus(ActionStatus.COMPLETED);
        action.setImplementedAction(impl);
        action.setStatus(ActionStatus.COMPLETED);
        completed.reopen(ctx);
        assertThat(action.getStatus()).isEqualTo(ActionStatus.REOPENED);
        assertThat(impl.getStatus()).isEqualTo(ActionStatus.REOPENED);
        verify(callbacks).onReopen(impl);
    }

    @Test
    void implement_fromCompleted_throwsIllegalTransition() {
        action.setStatus(ActionStatus.COMPLETED);
        assertThatThrownBy(() -> completed.implement(ctx))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    // --- ReopenedState ---

    @Test
    void complete_fromReopened_movesToCompleted() {
        ImplementedAction impl = new ImplementedAction();
        impl.setStatus(ActionStatus.REOPENED);
        action.setImplementedAction(impl);
        action.setStatus(ActionStatus.REOPENED);
        reopened.complete(ctx);
        assertThat(action.getStatus()).isEqualTo(ActionStatus.COMPLETED);
        verify(callbacks).onComplete(impl);
    }

    @Test
    void abandon_fromReopened_movesToAbandoned() {
        action.setStatus(ActionStatus.REOPENED);
        reopened.abandon(ctx);
        assertThat(action.getStatus()).isEqualTo(ActionStatus.ABANDONED);
    }

    @Test
    void suspend_fromReopened_throwsIllegalTransition() {
        action.setStatus(ActionStatus.REOPENED);
        assertThatThrownBy(() -> reopened.suspend(ctx, "reason"))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void registry_includesNewStates() {
        ProposedState ps = new ProposedState();
        SuspendedState ss = new SuspendedState();
        InProgressState ip = new InProgressState();
        CompletedState cs = new CompletedState();
        AbandonedState as = new AbandonedState();
        ActionStateRegistry reg = new ActionStateRegistry(ps, ss, ip, cs, as,
                pendingApproval, reopened);
        assertThat(reg.stateFor(ActionStatus.PENDING_APPROVAL).name())
                .isEqualTo("PENDING_APPROVAL");
        assertThat(reg.stateFor(ActionStatus.REOPENED).name())
                .isEqualTo("REOPENED");
    }
}
