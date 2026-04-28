package edu.iu.p532.rpl.engine.state;

import edu.iu.p532.rpl.domain.ActionStatus;
import edu.iu.p532.rpl.domain.ImplementedAction;
import edu.iu.p532.rpl.exception.IllegalStateTransitionException;
import org.springframework.stereotype.Component;

@Component
public class InProgressState implements ActionState {

    @Override
    public void implement(ActionContext ctx) {
        throw new IllegalStateTransitionException(name(), "implement");
    }

    @Override
    public void suspend(ActionContext ctx, String reason) {
        ctx.setSuspensionReason(reason);
        ctx.getCallbacks().openSuspension(ctx.getAction(), reason);
        ctx.getAction().setStatus(ActionStatus.SUSPENDED);
    }

    @Override
    public void resume(ActionContext ctx) {
        throw new IllegalStateTransitionException(name(), "resume");
    }

    @Override
    public void complete(ActionContext ctx) {
        ImplementedAction implemented = ctx.getAction().getImplementedAction();
        if (implemented == null) {
            throw new IllegalStateTransitionException(name(), "complete");
        }
        ctx.getCallbacks().onComplete(implemented);
        ctx.getAction().setStatus(ActionStatus.COMPLETED);
        implemented.setStatus(ActionStatus.COMPLETED);
    }

    @Override
    public void abandon(ActionContext ctx) {
        ctx.getAction().setStatus(ActionStatus.ABANDONED);
    }

    @Override
    public String name() { return ActionStatus.IN_PROGRESS.name(); }
}
