package edu.iu.p532.rpl.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "resource_allocation")
public class ResourceAllocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "action_id")
    private ProposedAction action;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resource_type_id")
    private ResourceType resourceType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationKind kind;

    private String assetId;
    private Instant periodStart;
    private Instant periodEnd;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProposedAction getAction() { return action; }
    public void setAction(ProposedAction action) { this.action = action; }
    public ResourceType getResourceType() { return resourceType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public AllocationKind getKind() { return kind; }
    public void setKind(AllocationKind kind) { this.kind = kind; }
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public Instant getPeriodStart() { return periodStart; }
    public void setPeriodStart(Instant periodStart) { this.periodStart = periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Instant periodEnd) { this.periodEnd = periodEnd; }
}
