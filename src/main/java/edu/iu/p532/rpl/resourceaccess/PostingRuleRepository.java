package edu.iu.p532.rpl.resourceaccess;

import edu.iu.p532.rpl.domain.PostingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostingRuleRepository extends JpaRepository<PostingRule, Long> {
    List<PostingRule> findByTriggerAccount_Id(Long triggerAccountId);
}
