package edu.iu.p532.rpl.domain;

import edu.iu.p532.rpl.engine.composite.PlanNode;
import edu.iu.p532.rpl.engine.composite.PlanNodeVisitor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proposed_action")
public class ProposedAction implements PlanNode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "protocol_id")
    private Protocol protocol;

    @ManyToOne
    @JoinColumn(name = "parent_plan_id")
    private Plan parentPlan;

    private String party;
    private Instant timeRef;
    private String location;

    /**
     * Persisted as the {@link ActionStatus} enum so the database row alone is
     * sufficient to recover the State; the runtime State object is resolved by
     * {@code ActionStateRegistry} from this field.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status = ActionStatus.PROPOSED;

    @OneToMany(mappedBy = "action", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResourceAllocation> allocations = new ArrayList<>();

    @OneToOne(mappedBy = "proposedAction", cascade = CascadeType.ALL, orphanRemoval = true)
    private ImplementedAction implementedAction;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Protocol getProtocol() { return protocol; }
    public void setProtocol(Protocol protocol) { this.protocol = protocol; }
    public Plan getParentPlan() { return parentPlan; }
    public void setParentPlan(Plan parentPlan) { this.parentPlan = parentPlan; }
    public String getParty() { return party; }
    public void setParty(String party) { this.party = party; }
    public Instant getTimeRef() { return timeRef; }
    public void setTimeRef(Instant timeRef) { this.timeRef = timeRef; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public void setStatus(ActionStatus status) { this.status = status; }
    public List<ResourceAllocation> getAllocations() { return allocations; }
    public void setAllocations(List<ResourceAllocation> allocations) { this.allocations = allocations; }
    public ImplementedAction getImplementedAction() { return implementedAction; }
    public void setImplementedAction(ImplementedAction implementedAction) { this.implementedAction = implementedAction; }

    @Override
    public ActionStatus getStatus() { return status; }

    @Override
    public boolean isLeaf() { return true; }

    @Override
    public BigDecimal getTotalAllocatedQuantity(ResourceType resourceType) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ResourceAllocation a : allocations) {
            if (a.getResourceType() != null
                    && a.getResourceType().getId() != null
                    && a.getResourceType().getId().equals(resourceType.getId())) {
                sum = sum.add(a.getQuantity());
            }
        }
        return sum;
    }

    @Override
    public void accept(PlanNodeVisitor visitor) {
        visitor.visit(this);
    }
}
