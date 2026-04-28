package edu.iu.p532.rpl.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "posting_rule")
public class PostingRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trigger_account_id")
    private Account triggerAccount;

    @ManyToOne(optional = false)
    @JoinColumn(name = "output_account_id")
    private Account outputAccount;

    @Column(nullable = false)
    private String strategyType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Account getTriggerAccount() { return triggerAccount; }
    public void setTriggerAccount(Account triggerAccount) { this.triggerAccount = triggerAccount; }
    public Account getOutputAccount() { return outputAccount; }
    public void setOutputAccount(Account outputAccount) { this.outputAccount = outputAccount; }
    public String getStrategyType() { return strategyType; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }
}
