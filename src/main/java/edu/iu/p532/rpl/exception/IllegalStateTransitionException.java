package edu.iu.p532.rpl.exception;

public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String fromState, String event) {
        super("Illegal transition: cannot apply event '" + event + "' to state '" + fromState + "'");
    }
}
