package edu.iu.p532.rpl.manager;

import edu.iu.p532.rpl.domain.Plan;
import edu.iu.p532.rpl.domain.ProposedAction;
import edu.iu.p532.rpl.dto.PlanMetricsDto;
import edu.iu.p532.rpl.engine.composite.PlanNode;
import edu.iu.p532.rpl.engine.visitor.CompletionRatioVisitor;
import edu.iu.p532.rpl.engine.visitor.ResourceCostVisitor;
import edu.iu.p532.rpl.engine.visitor.RiskScoreVisitor;
import edu.iu.p532.rpl.exception.NotFoundException;
import edu.iu.p532.rpl.resourceaccess.PlanRepository;
import edu.iu.p532.rpl.resourceaccess.ProposedActionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanMetricsManager {

    private final PlanRepository planRepo;
    private final ProposedActionRepository actionRepo;

    public PlanMetricsManager(PlanRepository planRepo, ProposedActionRepository actionRepo) {
        this.planRepo = planRepo;
        this.actionRepo = actionRepo;
    }

    @Transactional(readOnly = true)
    public PlanMetricsDto computeMetrics(Long nodeId) {
        PlanNode node = resolveNode(nodeId);
        eagerLoad(node);

        CompletionRatioVisitor crv = new CompletionRatioVisitor();
        ResourceCostVisitor rcv = new ResourceCostVisitor();
        RiskScoreVisitor rsv = new RiskScoreVisitor();

        node.accept(crv);
        node.accept(rcv);
        node.accept(rsv);

        return new PlanMetricsDto(
                nodeId,
                crv.getRatio(),
                crv.getTotalLeaves(),
                crv.getCompletedLeaves(),
                rcv.getTotalCost(),
                rsv.getScore()
        );
    }

    private PlanNode resolveNode(Long id) {
        Plan plan = planRepo.findById(id).orElse(null);
        if (plan != null) return plan;
        return actionRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan or Action " + id));
    }

    private void eagerLoad(PlanNode node) {
        if (node instanceof Plan plan) {
            plan.getChildActions().forEach(a -> a.getAllocations().size());
            plan.getChildPlans().forEach(this::eagerLoad);
        } else if (node instanceof ProposedAction action) {
            action.getAllocations().size();
        }
    }
}
