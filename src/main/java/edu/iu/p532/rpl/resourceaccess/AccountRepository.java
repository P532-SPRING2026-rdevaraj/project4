package edu.iu.p532.rpl.resourceaccess;

import edu.iu.p532.rpl.domain.Account;
import edu.iu.p532.rpl.domain.AccountKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByKind(AccountKind kind);
    Optional<Account> findFirstByKind(AccountKind kind);
}
