package edu.iu.p532.rpl.engine.ledger;

import edu.iu.p532.rpl.domain.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

@Component
public class ReversalLedgerEntryGenerator extends AbstractLedgerEntryGenerator {

    public ReversalLedgerEntryGenerator(Clock clock) {
        super(clock);
    }

    @Override
    protected List<ResourceAllocation> selectAllocations(ImplementedAction action) {
        return action.getProposedAction().getAllocations().stream()
                .filter(a -> a.getResourceType() != null
                        && a.getResourceType().getKind() == ResourceKind.CONSUMABLE)
                .toList();
    }

    @Override
    protected void validate(List<ResourceAllocation> allocs) {
        for (ResourceAllocation a : allocs) {
            if (a.getQuantity() == null || a.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                    "Allocation " + a.getId() + " must have a positive quantity for reversal");
            }
        }
    }

    @Override
    protected Entry buildWithdrawal(Transaction tx, ResourceAllocation a) {
        Entry e = new Entry();
        e.setTransaction(tx);
        e.setAmount(a.getQuantity()); // positive: restores pool
        e.setChargedAt(java.time.Instant.now(clock));
        e.setBookedAt(java.time.Instant.now(clock));
        e.setOriginatingAction(a.getAction());
        e.setAccount(a.getResourceType().getPoolAccount());
        return e;
    }

    @Override
    protected Entry buildDeposit(Transaction tx, ResourceAllocation a) {
        Entry e = new Entry();
        e.setTransaction(tx);
        e.setAmount(a.getQuantity().negate()); // negative: reversal credit
        e.setChargedAt(java.time.Instant.now(clock));
        e.setBookedAt(java.time.Instant.now(clock));
        e.setOriginatingAction(a.getAction());
        Account reversal = new Account();
        reversal.setKind(AccountKind.USAGE);
        reversal.setName("reversal:" + a.getAction().getId() + ":" + a.getResourceType().getName());
        reversal.setResourceType(a.getResourceType());
        reversal.setBalance(BigDecimal.ZERO);
        e.setAccount(reversal);
        return e;
    }
}
