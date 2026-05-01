package edu.iu.p532.rpl.resourceaccess;

import edu.iu.p532.rpl.domain.Protocol;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProtocolRepository extends JpaRepository<Protocol, Long> {

    @Override
    @EntityGraph(attributePaths = {"steps", "steps.subProtocol"})
    List<Protocol> findAll();

    @Override
    @EntityGraph(attributePaths = {"steps", "steps.subProtocol"})
    Optional<Protocol> findById(Long id);
}
