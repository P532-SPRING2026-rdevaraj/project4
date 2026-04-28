package edu.iu.p532.rpl.client;

import edu.iu.p532.rpl.domain.Plan;
import edu.iu.p532.rpl.domain.ProposedAction;
import edu.iu.p532.rpl.dto.*;
import edu.iu.p532.rpl.manager.PlanManager;
import edu.iu.p532.rpl.manager.PlanReportManager;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanManager planManager;
    private final PlanReportManager reportManager;

    public PlanController(PlanManager planManager, PlanReportManager reportManager) {
        this.planManager = planManager;
        this.reportManager = reportManager;
    }

    @GetMapping
    public List<PlanTreeView> listRoots() {
        List<PlanTreeView> roots = new ArrayList<>();
        for (Plan p : planManager.findRoots()) roots.add(toView(p));
        return roots;
    }

    @PostMapping
    public PlanTreeView create(@RequestBody CreatePlanRequest req) {
        Plan plan = req.protocolId() == null
                ? planManager.createFromScratch(req.name())
                : planManager.createFromProtocol(req.protocolId(), req.name());
        return toView(plan);
    }

    @GetMapping("/{id}")
    public PlanTreeView get(@PathVariable Long id) {
        return toView(planManager.findById(id));
    }

    @GetMapping("/{id}/report")
    public PlanReport report(@PathVariable Long id) {
        return reportManager.buildReport(id);
    }

    private PlanTreeView toView(Plan plan) {
        List<PlanTreeView> kids = new ArrayList<>();
        for (Plan c : plan.getChildPlans()) kids.add(toView(c));
        for (ProposedAction a : plan.getChildActions()) kids.add(toLeafView(a));
        return new PlanTreeView(plan.getId(), plan.getName(), "PLAN", plan.getStatus(), kids, List.of());
    }

    private PlanTreeView toLeafView(ProposedAction a) {
        ActionView.Implemented impl = a.getImplementedAction() == null
                ? null
                : new ActionView.Implemented(
                        a.getImplementedAction().getId(),
                        a.getImplementedAction().getActualStart(),
                        a.getImplementedAction().getActualParty(),
                        a.getImplementedAction().getActualLocation());
        ActionView leaf = new ActionView(
                a.getId(), a.getName(), a.getStatus(),
                a.getParty(), a.getLocation(), a.getTimeRef(), impl);
        return new PlanTreeView(a.getId(), a.getName(), "ACTION", a.getStatus(), List.of(), List.of(leaf));
    }
}
