package edu.iu.p532.rpl.engine.state;

/**
 * State-pattern interface. Each concrete implementation is a stateless Spring
 * singleton; all mutable data lives in {@link ActionContext}. This means that
 * adding a new state in Week 2 only requires writing a new implementation and
 * a single edit in whichever existing state introduces the new transition.
 */
public interface ActionState {
    void implement(ActionContext ctx);
    void suspend(ActionContext ctx, String reason);
    void resume(ActionContext ctx);
    void complete(ActionContext ctx);
    void abandon(ActionContext ctx);
    String name();
}
