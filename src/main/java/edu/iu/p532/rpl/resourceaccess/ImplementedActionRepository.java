package edu.iu.p532.rpl.resourceaccess;

import edu.iu.p532.rpl.domain.ImplementedAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImplementedActionRepository extends JpaRepository<ImplementedAction, Long> {
}
