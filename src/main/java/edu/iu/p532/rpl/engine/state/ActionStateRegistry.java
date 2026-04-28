package edu.iu.p532.rpl.engine.state;

import edu.iu.p532.rpl.domain.ActionStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Resolves the State singleton for a persisted {@link ActionStatus}. Adding a
 * new state in Week 2 only requires writing a new state bean and one extra
 * map entry here — no caller of {@link ActionStateRegistry#stateFor} changes.
 */
@Component
public class ActionStateRegistry {

    private final Map<ActionStatus, ActionState> states = new EnumMap<>(ActionStatus.class);

    public ActionStateRegistry(ProposedState proposed,
                               SuspendedState suspended,
                               InProgressState inProgress,
                               CompletedState completed,
                               AbandonedState abandoned) {
        states.put(ActionStatus.PROPOSED, proposed);
        states.put(ActionStatus.SUSPENDED, suspended);
        states.put(ActionStatus.IN_PROGRESS, inProgress);
        states.put(ActionStatus.COMPLETED, completed);
        states.put(ActionStatus.ABANDONED, abandoned);
    }

    public ActionState stateFor(ActionStatus status) {
        ActionState s = states.get(status);
        if (s == null) {
            throw new IllegalStateException("No state implementation for: " + status);
        }
        return s;
    }
}
