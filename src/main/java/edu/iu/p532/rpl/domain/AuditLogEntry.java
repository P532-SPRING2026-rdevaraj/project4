package edu.iu.p532.rpl.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log_entry")
public class AuditLogEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String event;

    private Long accountId;
    private Long entryId;
    private Long actionId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(length = 2000)
    private String detail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }
    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
