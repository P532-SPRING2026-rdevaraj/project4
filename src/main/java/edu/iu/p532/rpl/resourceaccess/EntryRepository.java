package edu.iu.p532.rpl.resourceaccess;

import edu.iu.p532.rpl.domain.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Long> {
    List<Entry> findByAccount_IdOrderByBookedAtDesc(Long accountId);
}
