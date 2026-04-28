package edu.iu.p532.rpl.ledger;

import edu.iu.p532.rpl.domain.*;
import edu.iu.p532.rpl.engine.ledger.ConsumableLedgerEntryGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ConsumableLedgerEntryGeneratorTest {

    private ConsumableLedgerEntryGenerator generator;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-04-27T10:00:00Z"), ZoneOffset.UTC);
        generator = new ConsumableLedgerEntryGenerator(fixedClock);
    }

    @Test
    void generateEntries_consumableAlloc_producesBalancedDoubleEntry() {
        // Arrange
        ResourceType rt = new ResourceType();
        rt.setName("steel");
        rt.setKind(ResourceKind.CONSUMABLE);
        Account pool = new Account();
        pool.setKind(AccountKind.POOL);
        pool.setName("pool:steel");
        pool.setBalance(new BigDecimal("100"));
        rt.setPoolAccount(pool);

        ProposedAction action = new ProposedAction();
        action.setId(42L);
        action.setName("weld");

        ResourceAllocation alloc = new ResourceAllocation();
        alloc.setAction(action);
        alloc.setResourceType(rt);
        alloc.setQuantity(new BigDecimal("7"));
        alloc.setKind(AllocationKind.GENERAL);
        action.getAllocations().add(alloc);

        ImplementedAction impl = new ImplementedAction();
        impl.setProposedAction(action);
        impl.setActualStart(Instant.now(fixedClock));

        // Act
        Transaction tx = generator.generateEntries(impl);

        // Assert: one withdrawal + one deposit, conservation = 0
        assertThat(tx.getEntries()).hasSize(2);
        BigDecimal sum = tx.getEntries().stream()
                .map(Entry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum.signum()).isZero();

        Entry withdrawal = tx.getEntries().stream().filter(e -> e.getAmount().signum() < 0).findFirst().orElseThrow();
        Entry deposit    = tx.getEntries().stream().filter(e -> e.getAmount().signum() > 0).findFirst().orElseThrow();
        assertThat(withdrawal.getAccount()).isSameAs(pool);
        assertThat(deposit.getAccount().getKind()).isEqualTo(AccountKind.USAGE);
    }

    @Test
    void generateEntries_skipsAssetAllocations() {
        // Arrange
        ResourceType assetType = new ResourceType();
        assetType.setKind(ResourceKind.ASSET);
        ProposedAction action = new ProposedAction();
        ResourceAllocation alloc = new ResourceAllocation();
        alloc.setAction(action);
        alloc.setResourceType(assetType);
        alloc.setQuantity(BigDecimal.ONE);
        alloc.setKind(AllocationKind.SPECIFIC);
        action.getAllocations().add(alloc);
        ImplementedAction impl = new ImplementedAction();
        impl.setProposedAction(action);

        // Act
        Transaction tx = generator.generateEntries(impl);

        // Assert: no entries because no consumable allocations
        assertThat(tx.getEntries()).isEmpty();
    }

    @Test
    void generateEntries_zeroQuantity_throws() {
        ResourceType rt = new ResourceType();
        rt.setKind(ResourceKind.CONSUMABLE);
        Account pool = new Account();
        pool.setKind(AccountKind.POOL);
        rt.setPoolAccount(pool);
        ProposedAction action = new ProposedAction();
        ResourceAllocation alloc = new ResourceAllocation();
        alloc.setAction(action);
        alloc.setResourceType(rt);
        alloc.setQuantity(BigDecimal.ZERO);
        alloc.setKind(AllocationKind.GENERAL);
        action.getAllocations().add(alloc);
        ImplementedAction impl = new ImplementedAction();
        impl.setProposedAction(action);

        assertThatThrownBy(() -> generator.generateEntries(impl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive quantity");
    }

    @Test
    void generateEntries_recordsClockTimestamps() {
        ResourceType rt = new ResourceType();
        rt.setKind(ResourceKind.CONSUMABLE);
        Account pool = new Account();
        pool.setKind(AccountKind.POOL);
        rt.setPoolAccount(pool);
        ProposedAction action = new ProposedAction();
        ResourceAllocation alloc = new ResourceAllocation();
        alloc.setAction(action);
        alloc.setResourceType(rt);
        alloc.setQuantity(BigDecimal.ONE);
        alloc.setKind(AllocationKind.GENERAL);
        action.getAllocations().add(alloc);
        ImplementedAction impl = new ImplementedAction();
        impl.setProposedAction(action);

        Transaction tx = generator.generateEntries(impl);

        Instant expected = Instant.parse("2026-04-27T10:00:00Z");
        assertThat(tx.getCreatedAt()).isEqualTo(expected);
        assertThat(tx.getEntries().get(0).getBookedAt()).isEqualTo(expected);
    }
}
