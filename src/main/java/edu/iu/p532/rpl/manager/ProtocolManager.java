package edu.iu.p532.rpl.manager;

import edu.iu.p532.rpl.domain.Protocol;
import edu.iu.p532.rpl.domain.ProtocolStep;
import edu.iu.p532.rpl.exception.NotFoundException;
import edu.iu.p532.rpl.resourceaccess.ProtocolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Knowledge-level Manager. CRUD over reusable Protocol templates. Kept apart
 * from operational Plan/Action managers per the knowledge/operational split.
 */
@Service
public class ProtocolManager {

    private final ProtocolRepository protocolRepo;

    public ProtocolManager(ProtocolRepository protocolRepo) {
        this.protocolRepo = protocolRepo;
    }

    @Transactional(readOnly = true)
    public List<Protocol> findAll() {
        return protocolRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Protocol findById(Long id) {
        return protocolRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Protocol " + id));
    }

    @Transactional
    public Protocol create(Protocol protocol) {
        if (protocol.getSteps() != null) {
            int idx = 0;
            for (ProtocolStep step : protocol.getSteps()) {
                step.setProtocol(protocol);
                step.setOrderIndex(idx++);
            }
        }
        return protocolRepo.save(protocol);
    }
}
