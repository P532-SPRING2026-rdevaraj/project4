package edu.iu.p532.rpl.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entry")
public class Entry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    /**
     * Signed amount: a withdrawal stores a negative number, a deposit stores a
     * positive number. Conservation across a transaction is enforced by the
     * template-method base class on post.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant chargedAt;

    @Column(nullable = false)
    private Instant bookedAt;

    @ManyToOne
    @JoinColumn(name = "originating_action_id")
    private ProposedAction originatingAction;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Instant getChargedAt() { return chargedAt; }
    public void setChargedAt(Instant chargedAt) { this.chargedAt = chargedAt; }
    public Instant getBookedAt() { return bookedAt; }
    public void setBookedAt(Instant bookedAt) { this.bookedAt = bookedAt; }
    public ProposedAction getOriginatingAction() { return originatingAction; }
    public void setOriginatingAction(ProposedAction originatingAction) { this.originatingAction = originatingAction; }
}
