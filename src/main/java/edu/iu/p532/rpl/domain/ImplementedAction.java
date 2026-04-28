package edu.iu.p532.rpl.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "implemented_action")
public class ImplementedAction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "proposed_action_id", unique = true)
    private ProposedAction proposedAction;

    private Instant actualStart;
    private String actualParty;
    private String actualLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status = ActionStatus.IN_PROGRESS;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProposedAction getProposedAction() { return proposedAction; }
    public void setProposedAction(ProposedAction proposedAction) { this.proposedAction = proposedAction; }
    public Instant getActualStart() { return actualStart; }
    public void setActualStart(Instant actualStart) { this.actualStart = actualStart; }
    public String getActualParty() { return actualParty; }
    public void setActualParty(String actualParty) { this.actualParty = actualParty; }
    public String getActualLocation() { return actualLocation; }
    public void setActualLocation(String actualLocation) { this.actualLocation = actualLocation; }
    public ActionStatus getStatus() { return status; }
    public void setStatus(ActionStatus status) { this.status = status; }
}
