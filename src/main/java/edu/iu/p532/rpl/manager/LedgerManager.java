package edu.iu.p532.rpl.manager;

import edu.iu.p532.rpl.domain.Account;
import edu.iu.p532.rpl.domain.Entry;
import edu.iu.p532.rpl.domain.ImplementedAction;
import edu.iu.p532.rpl.domain.Transaction;
import edu.iu.p532.rpl.engine.ledger.AbstractLedgerEntryGenerator;
import edu.iu.p532.rpl.engine.ledger.ConsumableLedgerEntryGenerator;
import edu.iu.p532.rpl.engine.posting.PostingRuleEngine;
import edu.iu.p532.rpl.resourceaccess.AccountRepository;
import edu.iu.p532.rpl.resourceaccess.EntryRepository;
import edu.iu.p532.rpl.resourceaccess.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operational Manager for the ledger. Selects an
 * {@link AbstractLedgerEntryGenerator} (Week 1 has only the consumable one;
 * Week 2 will add an asset generator chosen here without changing the State
 * machine), persists entries, updates pool balances, and fires the posting
 * rule.
 */
@Service
public class LedgerManager {

    private final ConsumableLedgerEntryGenerator consumableGen;
    private final TransactionRepository txRepo;
    private final EntryRepository entryRepo;
    private final AccountRepository accountRepo;
    private final PostingRuleEngine postingRules;

    public LedgerManager(ConsumableLedgerEntryGenerator consumableGen,
                         TransactionRepository txRepo,
                         EntryRepository entryRepo,
                         AccountRepository accountRepo,
                         PostingRuleEngine postingRules) {
        this.consumableGen = consumableGen;
        this.txRepo = txRepo;
        this.entryRepo = entryRepo;
        this.accountRepo = accountRepo;
        this.postingRules = postingRules;
    }

    @Transactional
    public Transaction postCompletion(ImplementedAction implemented) {
        // Week 1: only consumable generator. Week 2: a small selector here
        // (asset vs consumable) keeps the State machine ignorant of ledger
        // strategy choice.
        Transaction tx = consumableGen.generateEntries(implemented);
        Transaction saved = txRepo.save(tx);
        for (Entry e : saved.getEntries()) {
            Account acct = accountRepo.findById(e.getAccount().getId())
                    .orElseGet(() -> accountRepo.save(e.getAccount()));
            acct.setBalance(acct.getBalance().add(e.getAmount()));
            accountRepo.save(acct);
            e.setAccount(acct);
            entryRepo.save(e);
            postingRules.onEntryPosted(e);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Entry> entriesFor(Long accountId) {
        return entryRepo.findByAccount_IdOrderByBookedAtDesc(accountId);
    }

    @Transactional(readOnly = true)
    public List<Account> allAccounts() {
        return accountRepo.findAll();
    }
}
