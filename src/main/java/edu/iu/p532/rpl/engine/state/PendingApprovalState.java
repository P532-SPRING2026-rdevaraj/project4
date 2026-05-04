package edu.iu.p532.rpl.engine.state;

import edu.iu.p532.rpl.domain.ActionStatus;
import edu.iu.p532.rpl.exception.IllegalStateTransitionException;
import org.springframework.stereotype.Component;

@Component
public class PendingApprovalState implements ActionState {

    @Override public void implement(ActionContext ctx) {
        throw new IllegalStateTransitionException(name(), "implement");
    }

    @Override public void suspend(ActionContext ctx, String reason) {
        throw new IllegalStateTransitionException(name(), "suspend");
    }

    @Override public void resume(ActionContext ctx) {
        throw new IllegalStateTransitionException(name(), "resume");
    }

    @Override public void complete(ActionContext ctx) {
        throw new IllegalStateTransitionException(name(), "complete");
    }

    @Override public void abandon(ActionContext ctx) {
        throw new IllegalStateTransitionException(name(), "abandon");
    }

    @Override public void approve(ActionContext ctx) {
        ctx.getCallbacks().onImplement(ctx.getAction());
        ctx.getAction().setStatus(ActionStatus.IN_PROGRESS);
    }

    @Override public void reject(ActionContext ctx) {
        ctx.getAction().setStatus(ActionStatus.PROPOSED);
    }

    @Override public String name() { return ActionStatus.PENDING_APPROVAL.name(); }
}
