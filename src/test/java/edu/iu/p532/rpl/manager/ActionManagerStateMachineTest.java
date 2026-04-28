package edu.iu.p532.rpl.manager;

import edu.iu.p532.rpl.domain.ActionStatus;
import edu.iu.p532.rpl.domain.ImplementedAction;
import edu.iu.p532.rpl.domain.ProposedAction;
import edu.iu.p532.rpl.engine.state.*;
import edu.iu.p532.rpl.exception.IllegalStateTransitionException;
import edu.iu.p532.rpl.resourceaccess.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionManagerStateMachineTest {

    private ActionManager manager;
    private ProposedActionRepository actionRepo;
    private ImplementedActionRepository implementedRepo;
    private SuspensionRepository suspensionRepo;
    private LedgerManager ledgerManager;
    private ActionStateRegistry registry;

    @BeforeEach
    void setUp() {
        actionRepo = mock(ProposedActionRepository.class);
        implementedRepo = mock(ImplementedActionRepository.class);
        suspensionRepo = mock(SuspensionRepository.class);
        ResourceAllocationRepository allocationRepo = mock(ResourceAllocationRepository.class);
        ledgerManager = mock(LedgerManager.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-27T10:00:00Z"), ZoneOffset.UTC);
        registry = new ActionStateRegistry(
                new ProposedState(), new SuspendedState(), new InProgressState(),
                new CompletedState(), new AbandonedState());
        manager = new ActionManager(actionRepo, implementedRepo, suspensionRepo,
                allocationRepo, registry, ledgerManager, clock);
    }

    @Test
    void implement_createsImplementedActionAndMovesToInProgress() {
        // Arrange
        ProposedAction a = new ProposedAction();
        a.setId(7L);
        a.setStatus(ActionStatus.PROPOSED);
        when(actionRepo.findById(7L)).thenReturn(Optional.of(a));
        when(actionRepo.save(any(ProposedAction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(implementedRepo.save(any(ImplementedAction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProposedAction result = manager.implement(7L);

        // Assert
        assertThat(result.getStatus()).isEqualTo(ActionStatus.IN_PROGRESS);
        assertThat(result.getImplementedAction()).isNotNull();
        assertThat(result.getImplementedAction().getActualStart()).isEqualTo(Instant.parse("2026-04-27T10:00:00Z"));
    }

    @Test
    void completeWithoutImplemented_throwsIllegalTransition() {
        ProposedAction a = new ProposedAction();
        a.setId(8L);
        a.setStatus(ActionStatus.IN_PROGRESS);
        when(actionRepo.findById(8L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> manager.complete(8L))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void complete_postsLedgerEntries() {
        // Arrange
        ProposedAction a = new ProposedAction();
        a.setId(9L);
        a.setStatus(ActionStatus.IN_PROGRESS);
        ImplementedAction impl = new ImplementedAction();
        impl.setProposedAction(a);
        impl.setStatus(ActionStatus.IN_PROGRESS);
        a.setImplementedAction(impl);
        when(actionRepo.findById(9L)).thenReturn(Optional.of(a));
        when(actionRepo.save(any(ProposedAction.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        manager.complete(9L);

        // Assert
        verify(ledgerManager).postCompletion(impl);
        assertThat(a.getStatus()).isEqualTo(ActionStatus.COMPLETED);
    }
}
