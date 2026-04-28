package edu.iu.p532.rpl.state;

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
class ActionStateTransitionsTest {

    private ProposedState proposed;
    private SuspendedState suspended;
    private InProgressState inProgress;
    private CompletedState completed;
    private AbandonedState abandoned;

    private ProposedAction action;
    private ActionStateCallbacks callbacks;
    private ActionContext ctx;

    @BeforeEach
    void setUp() {
        // Arrange
        proposed = new ProposedState();
        suspended = new SuspendedState();
        inProgress = new InProgressState();
        completed = new CompletedState();
        abandoned = new AbandonedState();

        action = new ProposedAction();
        action.setStatus(ActionStatus.PROPOSED);
        callbacks = mock(ActionStateCallbacks.class);
        ctx = new ActionContext(action, callbacks);
    }

    @Test
    void implement_fromProposed_movesToInProgress() {
        // Arrange
        when(callbacks.onImplement(action)).thenReturn(new ImplementedAction());
        // Act
        proposed.implement(ctx);
        // Assert
        assertThat(action.getStatus()).isEqualTo(ActionStatus.IN_PROGRESS);
        verify(callbacks).onImplement(action);
    }

    @Test
    void suspend_fromProposed_movesToSuspendedAndOpensRecord() {
        // Act
        proposed.suspend(ctx, "weather");
        // Assert
        assertThat(action.getStatus()).isEqualTo(ActionStatus.SUSPENDED);
        verify(callbacks).openSuspension(action, "weather");
    }

    @Test
    void abandon_fromProposed_movesToAbandoned() {
        // Act
        proposed.abandon(ctx);
        // Assert
        assertThat(action.getStatus()).isEqualTo(ActionStatus.ABANDONED);
    }

    @Test
    void resume_fromProposed_throwsIllegalTransition() {
        // Act + Assert
        assertThatThrownBy(() -> proposed.resume(ctx))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void complete_fromProposed_throwsIllegalTransition() {
        assertThatThrownBy(() -> proposed.complete(ctx))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void resume_fromSuspended_movesBackToProposedAndClosesSuspension() {
        // Arrange
        action.setStatus(ActionStatus.SUSPENDED);
        // Act
        suspended.resume(ctx);
        // Assert
        assertThat(action.getStatus()).isEqualTo(ActionStatus.PROPOSED);
        verify(callbacks).closeSuspension(action);
    }

    @Test
    void abandon_fromSuspended_movesToAbandoned() {
        action.setStatus(ActionStatus.SUSPENDED);
        suspended.abandon(ctx);
        assertThat(action.getStatus()).isEqualTo(ActionStatus.ABANDONED);
    }

    @Test
    void implement_fromSuspended_throwsIllegalTransition() {
        assertThatThrownBy(() -> suspended.implement(ctx))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void complete_fromInProgress_movesToCompletedAndPostsLedger() {
        // Arrange
        ImplementedAction impl = new ImplementedAction();
        action.setImplementedAction(impl);
        action.setStatus(ActionStatus.IN_PROGRESS);
        // Act
        inProgress.complete(ctx);
        // Assert
        assertThat(action.getStatus()).isEqualTo(ActionStatus.COMPLETED);
        assertThat(impl.getStatus()).isEqualTo(ActionStatus.COMPLETED);
        verify(callbacks).onComplete(impl);
    }

    @Test
    void suspend_fromInProgress_movesToSuspended() {
        action.setStatus(ActionStatus.IN_PROGRESS);
        inProgress.suspend(ctx, "supplier delay");
        assertThat(action.getStatus()).isEqualTo(ActionStatus.SUSPENDED);
        verify(callbacks).openSuspension(action, "supplier delay");
    }

    @Test
    void anyTransition_fromCompleted_throwsIllegalTransition() {
        action.setStatus(ActionStatus.COMPLETED);
        assertThatThrownBy(() -> completed.implement(ctx))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(() -> completed.suspend(ctx, "x"))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(() -> completed.complete(ctx))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void anyTransition_fromAbandoned_throwsIllegalTransition() {
        action.setStatus(ActionStatus.ABANDONED);
        assertThatThrownBy(() -> abandoned.implement(ctx))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(() -> abandoned.resume(ctx))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void registry_returnsCorrectStateForEachStatus() {
        ActionStateRegistry reg = new ActionStateRegistry(proposed, suspended, inProgress, completed, abandoned);
        assertThat(reg.stateFor(ActionStatus.PROPOSED).name()).isEqualTo("PROPOSED");
        assertThat(reg.stateFor(ActionStatus.SUSPENDED).name()).isEqualTo("SUSPENDED");
        assertThat(reg.stateFor(ActionStatus.IN_PROGRESS).name()).isEqualTo("IN_PROGRESS");
        assertThat(reg.stateFor(ActionStatus.COMPLETED).name()).isEqualTo("COMPLETED");
        assertThat(reg.stateFor(ActionStatus.ABANDONED).name()).isEqualTo("ABANDONED");
    }
}
