package edu.iu.p532.rpl.resourceaccess;

import edu.iu.p532.rpl.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByParentPlanIsNull();
}
