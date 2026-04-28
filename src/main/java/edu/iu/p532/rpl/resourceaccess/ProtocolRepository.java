package edu.iu.p532.rpl.resourceaccess;

import edu.iu.p532.rpl.domain.Protocol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProtocolRepository extends JpaRepository<Protocol, Long> {
}
