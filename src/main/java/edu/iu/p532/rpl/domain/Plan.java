package edu.iu.p532.rpl.domain;

import edu.iu.p532.rpl.engine.composite.PlanNode;
import edu.iu.p532.rpl.engine.composite.PlanNodeVisitor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plan")
public class Plan implements PlanNode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "source_protocol_id")
    private Protocol sourceProtocol;

    private LocalDate targetStartDate;

    @ManyToOne
    @JoinColumn(name = "parent_plan_id")
    private Plan parentPlan;

    @OneToMany(mappedBy = "parentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Plan> childPlans = new ArrayList<>();

    @OneToMany(mappedBy = "parentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ProposedAction> childActions = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Protocol getSourceProtocol() { return sourceProtocol; }
    public void setSourceProtocol(Protocol sourceProtocol) { this.sourceProtocol = sourceProtocol; }
    public LocalDate getTargetStartDate() { return targetStartDate; }
    public void setTargetStartDate(LocalDate targetStartDate) { this.targetStartDate = targetStartDate; }
    public Plan getParentPlan() { return parentPlan; }
    public void setParentPlan(Plan parentPlan) { this.parentPlan = parentPlan; }
    public List<Plan> getChildPlans() { return childPlans; }
    public void setChildPlans(List<Plan> childPlans) { this.childPlans = childPlans; }
    public List<ProposedAction> getChildActions() { return childActions; }
    public void setChildActions(List<ProposedAction> childActions) { this.childActions = childActions; }

    @Transient
    public List<PlanNode> getChildren() {
        List<PlanNode> all = new ArrayList<>(childPlans.size() + childActions.size());
        all.addAll(childPlans);
        all.addAll(childActions);
        return all;
    }

    @Override
    public boolean isLeaf() { return false; }

    /**
     * Composite-derived status. COMPLETED only if every child is completed;
     * IN_PROGRESS if any child is in progress or completed (but not all);
     * SUSPENDED if any child is suspended and none in progress; ABANDONED if
     * all children are abandoned. Empty plans default to PROPOSED.
     */
    @Override
    public ActionStatus getStatus() {
        List<PlanNode> kids = getChildren();
        if (kids.isEmpty()) return ActionStatus.PROPOSED;

        boolean allCompleted = true;
        boolean allAbandoned = true;
        boolean anyInProgress = false;
        boolean anyCompleted = false;
        boolean anySuspended = false;

        for (PlanNode child : kids) {
            ActionStatus s = child.getStatus();
            if (s != ActionStatus.COMPLETED) allCompleted = false;
            if (s != ActionStatus.ABANDONED) allAbandoned = false;
            if (s == ActionStatus.IN_PROGRESS) anyInProgress = true;
            if (s == ActionStatus.COMPLETED) anyCompleted = true;
            if (s == ActionStatus.SUSPENDED) anySuspended = true;
        }

        if (allCompleted) return ActionStatus.COMPLETED;
        if (allAbandoned) return ActionStatus.ABANDONED;
        if (anyInProgress || anyCompleted) return ActionStatus.IN_PROGRESS;
        if (anySuspended) return ActionStatus.SUSPENDED;
        return ActionStatus.PROPOSED;
    }

    @Override
    public BigDecimal getTotalAllocatedQuantity(ResourceType resourceType) {
        BigDecimal sum = BigDecimal.ZERO;
        for (PlanNode child : getChildren()) {
            sum = sum.add(child.getTotalAllocatedQuantity(resourceType));
        }
        return sum;
    }

    @Override
    public void accept(PlanNodeVisitor visitor) {
        visitor.visit(this);
    }
}
