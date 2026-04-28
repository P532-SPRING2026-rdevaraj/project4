package edu.iu.p532.rpl.engine.ledger;

import edu.iu.p532.rpl.domain.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Template-method base class. {@code generateEntries} defines an immutable
 * skeleton — select, validate, create transaction, build/post entries, then
 * fire {@link #afterPost(Transaction)} as the only Week-2 extension point.
 *
 * <p>Conservation (a balanced double-entry transaction) is enforced inside
 * the {@code final} {@link #postEntries} method so subclasses cannot break
 * the ledger's accounting invariant.</p>
 */
public abstract class AbstractLedgerEntryGenerator {

    protected final Clock clock;

    protected AbstractLedgerEntryGenerator(Clock clock) {
        this.clock = clock;
    }

    public final Transaction generateEntries(ImplementedAction action) {
        List<ResourceAllocation> allocs = selectAllocations(action);
        validate(allocs);
        Transaction tx = createTransaction(action);
        for (ResourceAllocation a : allocs) {
            Entry withdrawal = buildWithdrawal(tx, a);
            Entry deposit    = buildDeposit(tx, a);
            postEntries(tx, withdrawal, deposit);
        }
        afterPost(tx);
        return tx;
    }

    protected abstract List<ResourceAllocation> selectAllocations(ImplementedAction action);

    protected abstract void validate(List<ResourceAllocation> allocs);

    protected Entry buildWithdrawal(Transaction tx, ResourceAllocation a) {
        Entry e = newEntry(tx, a, a.getQuantity().negate());
        e.setAccount(a.getResourceType().getPoolAccount());
        return e;
    }

    protected Entry buildDeposit(Transaction tx, ResourceAllocation a) {
        Entry e = newEntry(tx, a, a.getQuantity());
        Account usage = new Account();
        usage.setKind(AccountKind.USAGE);
        usage.setName("usage:" + a.getAction().getId() + ":" + a.getResourceType().getName());
        usage.setResourceType(a.getResourceType());
        usage.setBalance(BigDecimal.ZERO);
        e.setAccount(usage);
        return e;
    }

    /**
     * Hook intentionally empty in Week 1. Week 2's
     * {@code AssetLedgerEntryGenerator} will override this to write a
     * utilisation record without touching the base class.
     */
    protected void afterPost(Transaction tx) { }

    private Entry newEntry(Transaction tx, ResourceAllocation a, BigDecimal amount) {
        Entry e = new Entry();
        e.setTransaction(tx);
        e.setAmount(amount);
        Instant chargedAt = tx.getImplementedAction() != null
                && tx.getImplementedAction().getActualStart() != null
                ? Instant.now(clock)
                : Instant.now(clock);
        e.setChargedAt(chargedAt);
        e.setBookedAt(Instant.now(clock));
        e.setOriginatingAction(a.getAction());
        return e;
    }

    private Transaction createTransaction(ImplementedAction action) {
        Transaction tx = new Transaction();
        tx.setCreatedAt(Instant.now(clock));
        tx.setImplementedAction(action);
        tx.setDescription("Completion of action " + action.getProposedAction().getId());
        return tx;
    }

    private void postEntries(Transaction tx, Entry withdrawal, Entry deposit) {
        BigDecimal sum = withdrawal.getAmount().add(deposit.getAmount());
        if (sum.signum() != 0) {
            throw new IllegalStateException(
                "Conservation violated: entries must sum to zero, got " + sum);
        }
        tx.getEntries().add(withdrawal);
        tx.getEntries().add(deposit);
    }
}
