package edu.iu.p532.rpl.engine.posting;

import edu.iu.p532.rpl.domain.*;
import edu.iu.p532.rpl.resourceaccess.AccountRepository;
import edu.iu.p532.rpl.resourceaccess.AuditLogEntryRepository;
import edu.iu.p532.rpl.resourceaccess.EntryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

/**
 * Eager posting-rule engine. Called by the Manager whenever a new entry is
 * applied to a pool account. If the pool balance dips below zero an alert
 * entry is appended to a dedicated ALERT_MEMO account and an audit row is
 * recorded.
 */
@Component
public class PostingRuleEngine {

    private final AccountRepository accountRepo;
    private final EntryRepository entryRepo;
    private final AuditLogEntryRepository auditRepo;
    private final Clock clock;

    public PostingRuleEngine(AccountRepository accountRepo,
                             EntryRepository entryRepo,
                             AuditLogEntryRepository auditRepo,
                             Clock clock) {
        this.accountRepo = accountRepo;
        this.entryRepo = entryRepo;
        this.auditRepo = auditRepo;
        this.clock = clock;
    }

    public void onEntryPosted(Entry posted) {
        Account account = posted.getAccount();
        if (account.getKind() != AccountKind.POOL) return;
        if (account.getBalance().compareTo(BigDecimal.ZERO) >= 0) return;

        Account alertAccount = ensureAlertAccount(account);
        Entry alert = new Entry();
        alert.setTransaction(posted.getTransaction());
        alert.setAccount(alertAccount);
        alert.setAmount(account.getBalance());
        Instant now = Instant.now(clock);
        alert.setBookedAt(now);
        alert.setChargedAt(posted.getChargedAt());
        alert.setOriginatingAction(posted.getOriginatingAction());
        entryRepo.save(alert);

        AuditLogEntry log = new AuditLogEntry();
        log.setEvent("OVER_CONSUMPTION_ALERT");
        log.setAccountId(account.getId());
        log.setEntryId(alert.getId());
        log.setActionId(posted.getOriginatingAction() == null ? null : posted.getOriginatingAction().getId());
        log.setTimestamp(now);
        log.setDetail("Pool '" + account.getName() + "' balance " + account.getBalance());
        auditRepo.save(log);
    }

    private Account ensureAlertAccount(Account poolAccount) {
        String alertName = "alert:" + poolAccount.getName();
        return accountRepo.findAll().stream()
                .filter(a -> a.getKind() == AccountKind.ALERT_MEMO && alertName.equals(a.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Account alert = new Account();
                    alert.setKind(AccountKind.ALERT_MEMO);
                    alert.setName(alertName);
                    alert.setResourceType(poolAccount.getResourceType());
                    alert.setBalance(BigDecimal.ZERO);
                    return accountRepo.save(alert);
                });
    }
}
