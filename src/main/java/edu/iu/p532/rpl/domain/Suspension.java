package edu.iu.p532.rpl.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "suspension")
public class Suspension {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "proposed_action_id")
    private ProposedAction proposedAction;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false)
    private Instant startDate;

    private Instant endDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProposedAction getProposedAction() { return proposedAction; }
    public void setProposedAction(ProposedAction proposedAction) { this.proposedAction = proposedAction; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getStartDate() { return startDate; }
    public void setStartDate(Instant startDate) { this.startDate = startDate; }
    public Instant getEndDate() { return endDate; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }
}
