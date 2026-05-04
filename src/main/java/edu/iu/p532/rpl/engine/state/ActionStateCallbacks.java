package edu.iu.p532.rpl.engine.state;

import edu.iu.p532.rpl.domain.ImplementedAction;
import edu.iu.p532.rpl.domain.ProposedAction;

/**
 * Callback surface implemented by the Manager. State classes invoke these to
 * trigger persistent side-effects (creating an implemented action, posting
 * ledger entries, opening/closing a Suspension) without depending on the
 * Manager directly.
 */
public interface ActionStateCallbacks {
    ImplementedAction onImplement(ProposedAction action);
    void onComplete(ImplementedAction implemented);
    void openSuspension(ProposedAction action, String reason);
    void closeSuspension(ProposedAction action);
    default void onReopen(ImplementedAction implemented) {}
}
