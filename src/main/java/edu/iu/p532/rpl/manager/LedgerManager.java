package edu.iu.p532.rpl.manager;

import edu.iu.p532.rpl.domain.Account;
import edu.iu.p532.rpl.domain.Entry;
import edu.iu.p532.rpl.domain.ImplementedAction;
import edu.iu.p532.rpl.domain.Transaction;
import edu.iu.p532.rpl.engine.ledger.AbstractLedgerEntryGenerator;
import edu.iu.p532.rpl.engine.ledger.AssetLedgerEntryGenerator;
import edu.iu.p532.rpl.engine.ledger.ConsumableLedgerEntryGenerator;
import edu.iu.p532.rpl.engine.ledger.ReversalLedgerEntryGenerator;
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
    private final AssetLedgerEntryGenerator assetGen;
    private final ReversalLedgerEntryGenerator reversalGen;
    private final TransactionRepository txRepo;
    private final EntryRepository entryRepo;
    private final AccountRepository accountRepo;
    private final PostingRuleEngine postingRules;

    public LedgerManager(ConsumableLedgerEntryGenerator consumableGen,
                         AssetLedgerEntryGenerator assetGen,
                         ReversalLedgerEntryGenerator reversalGen,
                         TransactionRepository txRepo,
                         EntryRepository entryRepo,
                         AccountRepository accountRepo,
                         PostingRuleEngine postingRules) {
        this.consumableGen = consumableGen;
        this.assetGen = assetGen;
        this.reversalGen = reversalGen;
        this.txRepo = txRepo;
        this.entryRepo = entryRepo;
        this.accountRepo = accountRepo;
        this.postingRules = postingRules;
    }

    @Transactional
    public Transaction postCompletion(ImplementedAction implemented) {
        Transaction tx = persist(consumableGen.generateEntries(implemented));
        persist(assetGen.generateEntries(implemented));
        return tx;
    }

    @Transactional
    public Transaction postReversal(ImplementedAction implemented) {
        return persist(reversalGen.generateEntries(implemented));
    }

    private Transaction persist(Transaction tx) {
        for (Entry e : tx.getEntries()) {
            Account acct = e.getAccount();
            if (acct.getId() == null) {
                acct = accountRepo.save(acct);
                e.setAccount(acct);
            }
        }
        Transaction saved = txRepo.save(tx);
        for (Entry e : saved.getEntries()) {
            Account acct = e.getAccount();
            acct.setBalance(acct.getBalance().add(e.getAmount()));
            accountRepo.save(acct);
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
