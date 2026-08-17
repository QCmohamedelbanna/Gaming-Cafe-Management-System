package com.cafe.ps.service;

import com.cafe.ps.entity.AppUser;
import com.cafe.ps.entity.AccessRule;
import com.cafe.ps.entity.RulePermission;
import com.cafe.ps.entity.RolePermission;
import com.cafe.ps.repository.AppUserRepository;
import com.cafe.ps.repository.RolePermissionRepository;
import com.cafe.ps.repository.RulePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RulePermissionRepository rulePermissionRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        AccessRule rule = user.getRule();
        if (rule != null) {
            for (RulePermission rulePermission : rulePermissionRepository.findByRuleOrderByPermissionAsc(rule)) {
                authorities.add(new SimpleGrantedAuthority("PERMISSION_" + rulePermission.getPermission().name()));
            }
        } else {
            for (RolePermission rolePermission : rolePermissionRepository.findByRoleOrderByPermissionAsc(user.getRole())) {
                authorities.add(new SimpleGrantedAuthority("PERMISSION_" + rolePermission.getPermission().name()));
            }
        }

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!Boolean.TRUE.equals(user.getActive()))
                .build();
    }
}
