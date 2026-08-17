package com.cafe.ps.repository;

import com.cafe.ps.entity.AccessRule;
import com.cafe.ps.entity.RulePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RulePermissionRepository extends JpaRepository<RulePermission, Long> {

    List<RulePermission> findByRuleOrderByPermissionAsc(AccessRule rule);

    void deleteByRule(AccessRule rule);
}
