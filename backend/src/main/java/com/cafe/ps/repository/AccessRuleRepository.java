package com.cafe.ps.repository;

import com.cafe.ps.entity.AccessRule;
import com.cafe.ps.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccessRuleRepository extends JpaRepository<AccessRule, Long> {

    Optional<AccessRule> findByNameIgnoreCase(String name);

    Optional<AccessRule> findBySystemRole(Role systemRole);
}
