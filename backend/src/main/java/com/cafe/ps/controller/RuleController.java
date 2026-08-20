package com.cafe.ps.controller;

import com.cafe.ps.dto.CreateRuleRequest;
import com.cafe.ps.dto.RuleResponse;
import com.cafe.ps.dto.UpdateRuleRequest;
import com.cafe.ps.service.RuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERMISSION_PERMISSIONS_MANAGE')")
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    public List<RuleResponse> all() {
        return ruleService.getAll();
    }

    @PostMapping
    public RuleResponse create(@Valid @RequestBody CreateRuleRequest request) {
        return ruleService.create(request);
    }

    @PutMapping("/{id}")
    public RuleResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRuleRequest request
    ) {
        return ruleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        ruleService.delete(id);
    }
}
