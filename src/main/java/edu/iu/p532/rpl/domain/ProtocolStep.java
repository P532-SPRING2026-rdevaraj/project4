package edu.iu.p532.rpl.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "protocol_step")
public class ProtocolStep {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "protocol_id")
    private Protocol protocol;

    @Column(nullable = false)
    private String stepName;

    @ManyToOne
    @JoinColumn(name = "sub_protocol_id")
    private Protocol subProtocol;

    @Column(nullable = false)
    private int orderIndex;

    @ElementCollection
    @CollectionTable(name = "protocol_step_depends_on",
            joinColumns = @JoinColumn(name = "step_id"))
    @Column(name = "depends_on_step_name")
    private List<String> dependsOn = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Protocol getProtocol() { return protocol; }
    public void setProtocol(Protocol protocol) { this.protocol = protocol; }
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    public Protocol getSubProtocol() { return subProtocol; }
    public void setSubProtocol(Protocol subProtocol) { this.subProtocol = subProtocol; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public List<String> getDependsOn() { return dependsOn; }
    public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }
}
