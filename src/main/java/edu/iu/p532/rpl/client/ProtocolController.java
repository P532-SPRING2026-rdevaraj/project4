package edu.iu.p532.rpl.client;

import edu.iu.p532.rpl.domain.Protocol;
import edu.iu.p532.rpl.domain.ProtocolStep;
import edu.iu.p532.rpl.dto.CreateProtocolRequest;
import edu.iu.p532.rpl.exception.NotFoundException;
import edu.iu.p532.rpl.manager.ProtocolManager;
import edu.iu.p532.rpl.resourceaccess.ProtocolRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/protocols")
public class ProtocolController {

    private final ProtocolManager protocolManager;
    private final ProtocolRepository protocolRepo;

    public ProtocolController(ProtocolManager protocolManager, ProtocolRepository protocolRepo) {
        this.protocolManager = protocolManager;
        this.protocolRepo = protocolRepo;
    }

    @GetMapping
    public List<Protocol> list() {
        return protocolManager.findAll();
    }

    @PostMapping
    public Protocol create(@RequestBody CreateProtocolRequest req) {
        Protocol p = new Protocol();
        p.setName(req.name());
        p.setDescription(req.description());
        if (req.steps() != null) {
            List<ProtocolStep> steps = new ArrayList<>();
            for (CreateProtocolRequest.Step s : req.steps()) {
                ProtocolStep step = new ProtocolStep();
                step.setStepName(s.stepName());
                if (s.subProtocolId() != null) {
                    step.setSubProtocol(protocolRepo.findById(s.subProtocolId())
                            .orElseThrow(() -> new NotFoundException("Sub-protocol " + s.subProtocolId())));
                }
                step.setDependsOn(s.dependsOn() == null ? new ArrayList<>() : s.dependsOn());
                steps.add(step);
            }
            p.setSteps(steps);
        }
        return protocolManager.create(p);
    }

    @GetMapping("/{id}")
    public Protocol get(@PathVariable Long id) {
        return protocolManager.findById(id);
    }
}
