package edu.iu.p532.rpl.engine.state;

import edu.iu.p532.rpl.domain.ActionStatus;
import edu.iu.p532.rpl.exception.IllegalStateTransitionException;
import org.springframework.stereotype.Component;

@Component
public class SuspendedState implements ActionState {

    @Override
    public void implement(ActionContext ctx) {
        throw new IllegalStateTransitionException(name(), "implement");
    }

    @Override
    public void suspend(ActionContext ctx, String reason) {
        throw new IllegalStateTransitionException(name(), "suspend");
    }

    @Override
    public void resume(ActionContext ctx) {
        ctx.getCallbacks().closeSuspension(ctx.getAction());
        ctx.getAction().setStatus(ActionStatus.PROPOSED);
    }

    @Override
    public void complete(ActionContext ctx) {
        throw new IllegalStateTransitionException(name(), "complete");
    }

    @Override
    public void abandon(ActionContext ctx) {
        ctx.getCallbacks().closeSuspension(ctx.getAction());
        ctx.getAction().setStatus(ActionStatus.ABANDONED);
    }

    @Override
    public String name() { return ActionStatus.SUSPENDED.name(); }
}
