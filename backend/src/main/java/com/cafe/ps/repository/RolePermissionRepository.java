package com.cafe.ps.repository;

import com.cafe.ps.entity.Role;
import com.cafe.ps.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleOrderByPermissionAsc(Role role);

    void deleteByRole(Role role);
}
