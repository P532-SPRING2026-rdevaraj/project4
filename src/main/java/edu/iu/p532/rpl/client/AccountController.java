package edu.iu.p532.rpl.client;

import edu.iu.p532.rpl.domain.Account;
import edu.iu.p532.rpl.domain.Entry;
import edu.iu.p532.rpl.dto.AccountView;
import edu.iu.p532.rpl.dto.EntryView;
import edu.iu.p532.rpl.manager.LedgerManager;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final LedgerManager ledgerManager;

    public AccountController(LedgerManager ledgerManager) {
        this.ledgerManager = ledgerManager;
    }

    @GetMapping
    public List<AccountView> list() {
        List<AccountView> out = new ArrayList<>();
        for (Account a : ledgerManager.allAccounts()) {
            out.add(new AccountView(
                    a.getId(),
                    a.getName(),
                    a.getKind(),
                    a.getResourceType() == null ? null : a.getResourceType().getName(),
                    a.getResourceType() == null ? null : a.getResourceType().getUnit(),
                    a.getBalance()));
        }
        return out;
    }

    @GetMapping("/{id}/entries")
    public List<EntryView> entries(@PathVariable Long id) {
        List<EntryView> out = new ArrayList<>();
        for (Entry e : ledgerManager.entriesFor(id)) {
            out.add(new EntryView(
                    e.getId(),
                    e.getAccount().getId(),
                    e.getAmount(),
                    e.getChargedAt(),
                    e.getBookedAt(),
                    e.getOriginatingAction() == null ? null : e.getOriginatingAction().getId(),
                    e.getOriginatingAction() == null ? null : e.getOriginatingAction().getName()));
        }
        return out;
    }
}
