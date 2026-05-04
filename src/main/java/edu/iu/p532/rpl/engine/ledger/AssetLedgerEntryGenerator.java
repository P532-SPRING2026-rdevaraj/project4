package edu.iu.p532.rpl.engine.ledger;

import edu.iu.p532.rpl.domain.*;
import edu.iu.p532.rpl.resourceaccess.AuditLogEntryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class AssetLedgerEntryGenerator extends AbstractLedgerEntryGenerator {

    private final AuditLogEntryRepository auditRepo;

    public AssetLedgerEntryGenerator(Clock clock, AuditLogEntryRepository auditRepo) {
        super(clock);
        this.auditRepo = auditRepo;
    }

    @Override
    protected List<ResourceAllocation> selectAllocations(ImplementedAction action) {
        return action.getProposedAction().getAllocations().stream()
                .filter(a -> a.getResourceType() != null
                        && a.getResourceType().getKind() == ResourceKind.ASSET
                        && a.getKind() == AllocationKind.SPECIFIC)
                .toList();
    }

    @Override
    protected void validate(List<ResourceAllocation> allocs) {
        for (ResourceAllocation a : allocs) {
            if (a.getPeriodStart() == null || a.getPeriodEnd() == null) {
                throw new IllegalArgumentException(
                    "Asset allocation " + a.getId() + " must have a non-null time period");
            }
            long hours = Duration.between(a.getPeriodStart(), a.getPeriodEnd()).toHours();
            if (hours <= 0) {
                throw new IllegalArgumentException(
                    "Asset allocation " + a.getId() + " must have a positive duration in hours");
            }
        }
    }

    @Override
    protected Entry buildWithdrawal(Transaction tx, ResourceAllocation a) {
        BigDecimal hours = BigDecimal.valueOf(Duration.between(a.getPeriodStart(), a.getPeriodEnd()).toHours());
        Entry e = new Entry();
        e.setTransaction(tx);
        e.setAmount(hours.negate());
        e.setChargedAt(Instant.now(clock));
        e.setBookedAt(Instant.now(clock));
        e.setOriginatingAction(a.getAction());
        e.setAccount(a.getResourceType().getPoolAccount());
        return e;
    }

    @Override
    protected Entry buildDeposit(Transaction tx, ResourceAllocation a) {
        BigDecimal hours = BigDecimal.valueOf(Duration.between(a.getPeriodStart(), a.getPeriodEnd()).toHours());
        Entry e = new Entry();
        e.setTransaction(tx);
        e.setAmount(hours);
        e.setChargedAt(Instant.now(clock));
        e.setBookedAt(Instant.now(clock));
        e.setOriginatingAction(a.getAction());
        Account usage = new Account();
        usage.setKind(AccountKind.USAGE);
        usage.setName("asset-usage:" + a.getAction().getId() + ":" + a.getResourceType().getName());
        usage.setResourceType(a.getResourceType());
        usage.setBalance(BigDecimal.ZERO);
        e.setAccount(usage);
        return e;
    }

    @Override
    protected void afterPost(Transaction tx) {
        for (Entry e : tx.getEntries()) {
            if (e.getAccount() != null && e.getAccount().getKind() == AccountKind.USAGE
                    && e.getAccount().getName() != null
                    && e.getAccount().getName().startsWith("asset-usage:")) {
                AuditLogEntry log = new AuditLogEntry();
                log.setEvent("ASSET_UTILISATION");
                log.setActionId(e.getOriginatingAction() == null ? null : e.getOriginatingAction().getId());
                log.setTimestamp(Instant.now(clock));
                log.setDetail("Asset used for " + e.getAmount() + " hours on account " + e.getAccount().getName());
                auditRepo.save(log);
            }
        }
    }
}
