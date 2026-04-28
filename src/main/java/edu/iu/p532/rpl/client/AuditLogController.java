package edu.iu.p532.rpl.client;

import edu.iu.p532.rpl.domain.AuditLogEntry;
import edu.iu.p532.rpl.manager.AuditManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    private final AuditManager auditManager;

    public AuditLogController(AuditManager auditManager) {
        this.auditManager = auditManager;
    }

    @GetMapping
    public List<AuditLogEntry> list() {
        return auditManager.findAll();
    }
}
