package edu.iu.p532.rpl.engine.state;

import edu.iu.p532.rpl.domain.ProposedAction;

/**
 * Mutable context passed to each {@link ActionState} so the state objects
 * themselves can stay stateless singletons. The {@code callbacks} field
 * exposes side-effects (creating an ImplementedAction, posting ledger
 * entries, recording suspensions) without coupling state classes to the
 * Manager.
 */
public class ActionContext {
    private final ProposedAction action;
    private final ActionStateCallbacks callbacks;
    private String suspensionReason;

    public ActionContext(ProposedAction action, ActionStateCallbacks callbacks) {
        this.action = action;
        this.callbacks = callbacks;
    }

    public ProposedAction getAction() { return action; }
    public ActionStateCallbacks getCallbacks() { return callbacks; }
    public String getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(String reason) { this.suspensionReason = reason; }
}
