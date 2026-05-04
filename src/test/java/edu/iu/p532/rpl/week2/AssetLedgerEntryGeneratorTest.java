package edu.iu.p532.rpl.week2;

import edu.iu.p532.rpl.domain.*;
import edu.iu.p532.rpl.engine.ledger.AssetLedgerEntryGenerator;
import edu.iu.p532.rpl.resourceaccess.AuditLogEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetLedgerEntryGeneratorTest {

    private AssetLedgerEntryGenerator generator;
    private Clock clock;

    @Mock
    private AuditLogEntryRepository auditRepo;

    private ProposedAction action;
    private ImplementedAction implemented;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        generator = new AssetLedgerEntryGenerator(clock, auditRepo);

        Account pool = new Account();
        pool.setId(1L);
        pool.setKind(AccountKind.POOL);
        pool.setName("pool:equipment");
        pool.setBalance(BigDecimal.valueOf(100));

        ResourceType rt = new ResourceType();
        rt.setId(1L);
        rt.setName("equipment");
        rt.setKind(ResourceKind.ASSET);
        rt.setPoolAccount(pool);

        ResourceAllocation alloc = new ResourceAllocation();
        alloc.setId(1L);
        alloc.setResourceType(rt);
        alloc.setKind(AllocationKind.SPECIFIC);
        alloc.setQuantity(BigDecimal.ONE);
        alloc.setPeriodStart(Instant.parse("2026-01-01T08:00:00Z"));
        alloc.setPeriodEnd(Instant.parse("2026-01-01T12:00:00Z")); // 4 hours

        action = new ProposedAction();
        action.setId(10L);
        action.setAllocations(new ArrayList<>());
        action.getAllocations().add(alloc);
        alloc.setAction(action);

        implemented = new ImplementedAction();
        implemented.setProposedAction(action);
        implemented.setActualStart(Instant.now(clock));
    }

    @Test
    void generateEntries_specificAsset_usesHoursAsAmount() {
        // Arrange + Act
        Transaction tx = generator.generateEntries(implemented);
        // Assert: 4 hours → withdrawal -4, deposit +4
        assertThat(tx.getEntries()).hasSize(2);
        BigDecimal withdrawal = tx.getEntries().stream()
                .filter(e -> e.getAmount().signum() < 0).findFirst().orElseThrow().getAmount();
        assertThat(withdrawal).isEqualByComparingTo(BigDecimal.valueOf(-4));
    }

    @Test
    void generateEntries_specificAsset_writesAuditLog() {
        when(auditRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        generator.generateEntries(implemented);
        verify(auditRepo, atLeastOnce()).save(any(AuditLogEntry.class));
    }

    @Test
    void validate_missingPeriod_throwsIllegalArgument() {
        // Arrange: allocation without time period
        ResourceAllocation bad = action.getAllocations().get(0);
        bad.setPeriodEnd(null);
        // Act + Assert
        assertThatThrownBy(() -> generator.generateEntries(implemented))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-null time period");
    }

    @Test
    void validate_zeroDuration_throwsIllegalArgument() {
        ResourceAllocation bad = action.getAllocations().get(0);
        bad.setPeriodEnd(bad.getPeriodStart()); // zero hours
        assertThatThrownBy(() -> generator.generateEntries(implemented))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive duration");
    }

    @Test
    void selectAllocations_skipsGeneralAndConsumable() {
        // Add a GENERAL allocation and a CONSUMABLE — should be ignored
        ResourceAllocation general = new ResourceAllocation();
        general.setKind(AllocationKind.GENERAL);
        ResourceType consumableRt = new ResourceType();
        consumableRt.setKind(ResourceKind.CONSUMABLE);
        general.setResourceType(consumableRt);
        action.getAllocations().add(general);

        Transaction tx = generator.generateEntries(implemented);
        // Only the SPECIFIC ASSET allocation produces entries (2 entries)
        assertThat(tx.getEntries()).hasSize(2);
    }
}
