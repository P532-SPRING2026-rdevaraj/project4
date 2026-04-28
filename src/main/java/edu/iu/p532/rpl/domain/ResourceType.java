package edu.iu.p532.rpl.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "resource_type")
public class ResourceType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceKind kind;

    @Column(nullable = false)
    private String unit;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "pool_account_id")
    private Account poolAccount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ResourceKind getKind() { return kind; }
    public void setKind(ResourceKind kind) { this.kind = kind; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Account getPoolAccount() { return poolAccount; }
    public void setPoolAccount(Account poolAccount) { this.poolAccount = poolAccount; }
}
