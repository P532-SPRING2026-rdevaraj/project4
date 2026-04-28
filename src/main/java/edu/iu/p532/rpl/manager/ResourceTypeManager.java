package edu.iu.p532.rpl.manager;

import edu.iu.p532.rpl.domain.Account;
import edu.iu.p532.rpl.domain.AccountKind;
import edu.iu.p532.rpl.domain.ResourceType;
import edu.iu.p532.rpl.exception.NotFoundException;
import edu.iu.p532.rpl.resourceaccess.AccountRepository;
import edu.iu.p532.rpl.resourceaccess.ResourceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Knowledge-level Manager for ResourceType. Creating a resource type also
 * creates its linked POOL Account so callers can immediately seed inventory.
 */
@Service
public class ResourceTypeManager {

    private final ResourceTypeRepository resourceTypeRepo;
    private final AccountRepository accountRepo;

    public ResourceTypeManager(ResourceTypeRepository resourceTypeRepo, AccountRepository accountRepo) {
        this.resourceTypeRepo = resourceTypeRepo;
        this.accountRepo = accountRepo;
    }

    @Transactional(readOnly = true)
    public List<ResourceType> findAll() {
        return resourceTypeRepo.findAll();
    }

    @Transactional(readOnly = true)
    public ResourceType findById(Long id) {
        return resourceTypeRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("ResourceType " + id));
    }

    @Transactional
    public ResourceType create(ResourceType rt, BigDecimal initialPoolBalance) {
        Account pool = new Account();
        pool.setKind(AccountKind.POOL);
        pool.setName("pool:" + rt.getName());
        pool.setBalance(initialPoolBalance == null ? BigDecimal.ZERO : initialPoolBalance);
        rt.setPoolAccount(pool);
        ResourceType saved = resourceTypeRepo.save(rt);
        pool.setResourceType(saved);
        accountRepo.save(pool);
        return saved;
    }
}
