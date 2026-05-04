package edu.iu.p532.rpl.manager;

import edu.iu.p532.rpl.domain.*;
import edu.iu.p532.rpl.engine.state.ActionContext;
import edu.iu.p532.rpl.engine.state.ActionState;
import edu.iu.p532.rpl.engine.state.ActionStateCallbacks;
import edu.iu.p532.rpl.engine.state.ActionStateRegistry;
import edu.iu.p532.rpl.exception.NotFoundException;
import edu.iu.p532.rpl.resourceaccess.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Operational Manager for ProposedAction. Drives the State machine and
 * delegates ledger generation to {@link LedgerManager} via the callback
 * surface. The Manager is the only thing that knows about the registry, the
 * repositories, and the lifecycle policies — Engines stay focused.
 */
@Service
public class ActionManager implements ActionStateCallbacks {

    private final ProposedActionRepository actionRepo;
    private final ImplementedActionRepository implementedRepo;
    private final SuspensionRepository suspensionRepo;
    private final ResourceAllocationRepository allocationRepo;
    private final ActionStateRegistry stateRegistry;
    private final LedgerManager ledgerManager;
    private final Clock clock;

    public ActionManager(ProposedActionRepository actionRepo,
                         ImplementedActionRepository implementedRepo,
                         SuspensionRepository suspensionRepo,
                         ResourceAllocationRepository allocationRepo,
                         ActionStateRegistry stateRegistry,
                         LedgerManager ledgerManager,
                         Clock clock) {
        this.actionRepo = actionRepo;
        this.implementedRepo = implementedRepo;
        this.suspensionRepo = suspensionRepo;
        this.allocationRepo = allocationRepo;
        this.stateRegistry = stateRegistry;
        this.ledgerManager = ledgerManager;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProposedAction find(Long id) {
        return actionRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Action " + id));
    }

    @Transactional
    public ProposedAction implement(Long id) {
        return apply(id, (state, ctx) -> state.implement(ctx));
    }

    @Transactional
    public ProposedAction complete(Long id) {
        return apply(id, (state, ctx) -> state.complete(ctx));
    }

    @Transactional
    public ProposedAction suspend(Long id, String reason) {
        return apply(id, (state, ctx) -> state.suspend(ctx, reason));
    }

    @Transactional
    public ProposedAction resume(Long id) {
        return apply(id, (state, ctx) -> state.resume(ctx));
    }

    @Transactional
    public ProposedAction abandon(Long id) {
        return apply(id, (state, ctx) -> state.abandon(ctx));
    }

    @Transactional
    public ProposedAction submitForApproval(Long id) {
        return apply(id, (state, ctx) -> state.submitForApproval(ctx));
    }

    @Transactional
    public ProposedAction approve(Long id) {
        return apply(id, (state, ctx) -> state.approve(ctx));
    }

    @Transactional
    public ProposedAction reject(Long id) {
        return apply(id, (state, ctx) -> state.reject(ctx));
    }

    @Transactional
    public ProposedAction reopen(Long id) {
        return apply(id, (state, ctx) -> state.reopen(ctx));
    }

    @Transactional
    public ResourceAllocation addAllocation(Long actionId, ResourceAllocation alloc) {
        ProposedAction action = find(actionId);
        alloc.setAction(action);
        return allocationRepo.save(alloc);
    }

    private ProposedAction apply(Long id, StateOp op) {
        ProposedAction action = find(id);
        ActionState state = stateRegistry.stateFor(action.getStatus());
        ActionContext ctx = new ActionContext(action, this);
        op.run(state, ctx);
        return actionRepo.save(action);
    }

    @FunctionalInterface
    private interface StateOp {
        void run(ActionState state, ActionContext ctx);
    }

    // --- ActionStateCallbacks --------------------------------------------------

    @Override
    public ImplementedAction onImplement(ProposedAction action) {
        ImplementedAction impl = action.getImplementedAction();
        if (impl == null) {
            impl = new ImplementedAction();
            impl.setProposedAction(action);
            action.setImplementedAction(impl);
        }
        impl.setActualStart(Instant.now(clock));
        impl.setActualParty(action.getParty());
        impl.setActualLocation(action.getLocation());
        impl.setStatus(ActionStatus.IN_PROGRESS);
        return implementedRepo.save(impl);
    }

    @Override
    public void onComplete(ImplementedAction implemented) {
        ledgerManager.postCompletion(implemented);
    }

    @Override
    public void onReopen(ImplementedAction implemented) {
        if (implemented != null) ledgerManager.postReversal(implemented);
    }

    @Override
    public void openSuspension(ProposedAction action, String reason) {
        Suspension s = new Suspension();
        s.setProposedAction(action);
        s.setReason(reason);
        s.setStartDate(Instant.now(clock));
        suspensionRepo.save(s);
    }

    @Override
    public void closeSuspension(ProposedAction action) {
        List<Suspension> open = suspensionRepo.findAll().stream()
                .filter(s -> s.getProposedAction().getId().equals(action.getId())
                          && s.getEndDate() == null)
                .toList();
        for (Suspension s : open) {
            s.setEndDate(Instant.now(clock));
            suspensionRepo.save(s);
        }
    }
}
