package edu.iu.p532.rpl.client;

import edu.iu.p532.rpl.domain.ResourceType;
import edu.iu.p532.rpl.dto.CreateResourceTypeRequest;
import edu.iu.p532.rpl.manager.ResourceTypeManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resource-types")
public class ResourceTypeController {

    private final ResourceTypeManager manager;

    public ResourceTypeController(ResourceTypeManager manager) {
        this.manager = manager;
    }

    @GetMapping
    public List<ResourceType> list() {
        return manager.findAll();
    }

    @PostMapping
    public ResourceType create(@RequestBody CreateResourceTypeRequest req) {
        ResourceType rt = new ResourceType();
        rt.setName(req.name());
        rt.setKind(req.kind());
        rt.setUnit(req.unit());
        return manager.create(rt, req.initialPoolBalance());
    }
}
