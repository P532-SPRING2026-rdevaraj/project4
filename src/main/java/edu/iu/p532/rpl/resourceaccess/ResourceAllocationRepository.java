package edu.iu.p532.rpl.resourceaccess;

import edu.iu.p532.rpl.domain.ResourceAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceAllocationRepository extends JpaRepository<ResourceAllocation, Long> {
}
