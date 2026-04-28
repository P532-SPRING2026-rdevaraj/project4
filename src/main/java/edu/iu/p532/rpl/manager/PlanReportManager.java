package edu.iu.p532.rpl.manager;

import edu.iu.p532.rpl.domain.Plan;
import edu.iu.p532.rpl.domain.ResourceType;
import edu.iu.p532.rpl.dto.PlanReport;
import edu.iu.p532.rpl.dto.PlanReportRow;
import edu.iu.p532.rpl.engine.composite.PlanNode;
import edu.iu.p532.rpl.engine.iterator.DepthFirstPlanIterator;
import edu.iu.p532.rpl.exception.NotFoundException;
import edu.iu.p532.rpl.resourceaccess.PlanRepository;
import edu.iu.p532.rpl.resourceaccess.ResourceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builds the F10 plan-summary report by walking the composite tree with the
 * DFS iterator and asking each node for its derived state and totals through
 * the {@code PlanNode} interface — leaves and composites are visited
 * uniformly.
 */
@Service
public class PlanReportManager {

    private final PlanRepository planRepo;
    private final ResourceTypeRepository resourceTypeRepo;

    public PlanReportManager(PlanRepository planRepo, ResourceTypeRepository resourceTypeRepo) {
        this.planRepo = planRepo;
        this.resourceTypeRepo = resourceTypeRepo;
    }

    @Transactional(readOnly = true)
    public PlanReport buildReport(Long planId) {
        Plan root = planRepo.findById(planId)
                .orElseThrow(() -> new NotFoundException("Plan " + planId));
        eagerLoad(root);
        List<ResourceType> types = resourceTypeRepo.findAll();

        List<PlanReportRow> rows = new ArrayList<>();
        Iterator<PlanNode> it = new DepthFirstPlanIterator(root);
        while (it.hasNext()) {
            PlanNode node = it.next();
            Map<String, BigDecimal> totals = new HashMap<>();
            for (ResourceType rt : types) {
                BigDecimal qty = node.getTotalAllocatedQuantity(rt);
                if (qty.signum() != 0) totals.put(rt.getName(), qty);
            }
            rows.add(new PlanReportRow(
                    node.getId(),
                    node.getName(),
                    node.isLeaf() ? "ACTION" : "PLAN",
                    node.getStatus().name(),
                    totals));
        }
        return new PlanReport(root.getId(), root.getName(), rows);
    }

    private void eagerLoad(Plan plan) {
        plan.getChildActions().forEach(a -> a.getAllocations().size());
        for (Plan child : plan.getChildPlans()) eagerLoad(child);
    }
}
