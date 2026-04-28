package edu.iu.p532.rpl.manager;

import edu.iu.p532.rpl.domain.AuditLogEntry;
import edu.iu.p532.rpl.resourceaccess.AuditLogEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditManager {

    private final AuditLogEntryRepository repo;

    public AuditManager(AuditLogEntryRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntry> findAll() {
        return repo.findAllByOrderByTimestampDesc();
    }
}
