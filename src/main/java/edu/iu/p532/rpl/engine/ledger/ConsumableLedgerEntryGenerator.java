package edu.iu.p532.rpl.engine.ledger;

import edu.iu.p532.rpl.domain.ImplementedAction;
import edu.iu.p532.rpl.domain.ResourceAllocation;
import edu.iu.p532.rpl.domain.ResourceKind;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/**
 * Concrete Week-1 generator for CONSUMABLE resources. Hooks: it filters to
 * consumable allocations and validates positive quantities. The base class's
 * skeleton handles transaction creation, entry construction, conservation
 * check, and the (empty) afterPost hook.
 */
@Component
public class ConsumableLedgerEntryGenerator extends AbstractLedgerEntryGenerator {

    public ConsumableLedgerEntryGenerator(Clock clock) {
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
                    "Allocation " + a.getId() + " must have a positive quantity");
            }
        }
    }
}
