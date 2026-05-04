package edu.iu.p532.rpl.client;

import edu.iu.p532.rpl.domain.ProposedAction;
import edu.iu.p532.rpl.domain.ResourceAllocation;
import edu.iu.p532.rpl.dto.ActionView;
import edu.iu.p532.rpl.dto.CreateAllocationRequest;
import edu.iu.p532.rpl.dto.SuspendRequest;
import edu.iu.p532.rpl.exception.NotFoundException;
import edu.iu.p532.rpl.manager.ActionManager;
import edu.iu.p532.rpl.resourceaccess.ResourceTypeRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/actions")
public class ActionController {

    private final ActionManager actionManager;
    private final ResourceTypeRepository resourceTypeRepo;

    public ActionController(ActionManager actionManager, ResourceTypeRepository resourceTypeRepo) {
        this.actionManager = actionManager;
        this.resourceTypeRepo = resourceTypeRepo;
    }

    @GetMapping("/{id}")
    public ActionView get(@PathVariable Long id) {
        return toView(actionManager.find(id));
    }

    @PostMapping("/{id}/implement")
    public ActionView implement(@PathVariable Long id) {
        return toView(actionManager.implement(id));
    }

    @PostMapping("/{id}/complete")
    public ActionView complete(@PathVariable Long id) {
        return toView(actionManager.complete(id));
    }

    @PostMapping("/{id}/suspend")
    public ActionView suspend(@PathVariable Long id, @RequestBody(required = false) SuspendRequest req) {
        String reason = req == null ? null : req.reason();
        return toView(actionManager.suspend(id, reason));
    }

    @PostMapping("/{id}/resume")
    public ActionView resume(@PathVariable Long id) {
        return toView(actionManager.resume(id));
    }

    @PostMapping("/{id}/abandon")
    public ActionView abandon(@PathVariable Long id) {
        return toView(actionManager.abandon(id));
    }

    @PostMapping("/{id}/submit-for-approval")
    public ActionView submitForApproval(@PathVariable Long id) {
        return toView(actionManager.submitForApproval(id));
    }

    @PostMapping("/{id}/approve")
    public ActionView approve(@PathVariable Long id) {
        return toView(actionManager.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ActionView reject(@PathVariable Long id) {
        return toView(actionManager.reject(id));
    }

    @PostMapping("/{id}/reopen")
    public ActionView reopen(@PathVariable Long id) {
        return toView(actionManager.reopen(id));
    }

    @PostMapping("/{id}/allocations")
    public ResourceAllocation addAllocation(@PathVariable Long id, @RequestBody CreateAllocationRequest req) {
        ResourceAllocation a = new ResourceAllocation();
        a.setResourceType(resourceTypeRepo.findById(req.resourceTypeId())
                .orElseThrow(() -> new NotFoundException("ResourceType " + req.resourceTypeId())));
        a.setQuantity(req.quantity());
        a.setKind(req.kind());
        a.setAssetId(req.assetId());
        a.setPeriodStart(req.periodStart());
        a.setPeriodEnd(req.periodEnd());
        return actionManager.addAllocation(id, a);
    }

    private ActionView toView(ProposedAction a) {
        ActionView.Implemented impl = a.getImplementedAction() == null
                ? null
                : new ActionView.Implemented(
                        a.getImplementedAction().getId(),
                        a.getImplementedAction().getActualStart(),
                        a.getImplementedAction().getActualParty(),
                        a.getImplementedAction().getActualLocation());
        return new ActionView(a.getId(), a.getName(), a.getStatus(),
                a.getParty(), a.getLocation(), a.getTimeRef(), impl);
    }
}
