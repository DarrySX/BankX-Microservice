package com.bankx.transactions.infrastructure.adapter.out.legacyrisk;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskRuleJpaRepository extends JpaRepository<RiskRule, Long> {
    Optional<RiskRule> findFirstByCurrency(String currency);
}
