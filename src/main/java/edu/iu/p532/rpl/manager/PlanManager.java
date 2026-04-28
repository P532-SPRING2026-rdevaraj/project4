package edu.iu.p532.rpl.manager;

import edu.iu.p532.rpl.domain.*;
import edu.iu.p532.rpl.engine.composite.PlanNode;
import edu.iu.p532.rpl.engine.iterator.DepthFirstPlanIterator;
import edu.iu.p532.rpl.exception.NotFoundException;
import edu.iu.p532.rpl.resourceaccess.PlanRepository;
import edu.iu.p532.rpl.resourceaccess.ProtocolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Operational Manager. Owns Plan creation (from a Protocol or from scratch)
 * and exposes a DFS iterator over the loaded composite tree. Per the design
 * hint, the Manager loads the subtree before handing it to the iterator —
 * the iterator never queries the database.
 */
@Service
public class PlanManager {

    private final PlanRepository planRepo;
    private final ProtocolRepository protocolRepo;

    public PlanManager(PlanRepository planRepo, ProtocolRepository protocolRepo) {
        this.planRepo = planRepo;
        this.protocolRepo = protocolRepo;
    }

    @Transactional(readOnly = true)
    public List<Plan> findRoots() {
        return planRepo.findByParentPlanIsNull();
    }

    @Transactional(readOnly = true)
    public Plan findById(Long id) {
        return planRepo.findById(id).orElseThrow(() -> new NotFoundException("Plan " + id));
    }

    @Transactional
    public Plan createFromScratch(String name) {
        Plan plan = new Plan();
        plan.setName(name);
        return planRepo.save(plan);
    }

    @Transactional
    public Plan createFromProtocol(Long protocolId, String planName) {
        Protocol protocol = protocolRepo.findById(protocolId)
                .orElseThrow(() -> new NotFoundException("Protocol " + protocolId));
        Plan plan = new Plan();
        plan.setName(planName);
        plan.setSourceProtocol(protocol);

        // Generate one ProposedAction per step of the protocol, copying
        // dependency information into action names so Week-2 dependency-graph
        // logic has the data without an additional join table.
        for (ProtocolStep step : protocol.getSteps()) {
            ProposedAction action = new ProposedAction();
            action.setName(step.getStepName());
            action.setProtocol(step.getSubProtocol() != null ? step.getSubProtocol() : protocol);
            action.setParentPlan(plan);
            action.setStatus(ActionStatus.PROPOSED);
            plan.getChildActions().add(action);
        }
        return planRepo.save(plan);
    }

    @Transactional(readOnly = true)
    public List<PlanNode> depthFirst(Long planId) {
        Plan root = findById(planId);
        // Force load by walking children (lazy ManyToOne is fine for parent ref).
        eagerLoad(root);
        List<PlanNode> out = new ArrayList<>();
        Iterator<PlanNode> it = new DepthFirstPlanIterator(root);
        while (it.hasNext()) out.add(it.next());
        return out;
    }

    private void eagerLoad(Plan plan) {
        plan.getChildActions().size();
        for (Plan child : plan.getChildPlans()) {
            eagerLoad(child);
        }
    }
}
