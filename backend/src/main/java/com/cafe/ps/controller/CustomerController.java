package com.cafe.ps.controller;

import com.cafe.ps.dto.CustomerRequest;
import com.cafe.ps.dto.CustomerResponse;
import com.cafe.ps.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERMISSION_RESERVATIONS_MANAGE')")
public class CustomerController {

    private final CustomerService service;

    @GetMapping
    public List<CustomerResponse> search(@RequestParam(required = false) String q) {
        return service.search(q).stream().map(CustomerResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return CustomerResponse.from(service.get(id));
    }

    @PostMapping
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request
    ) {
        return CustomerResponse.from(service.update(id, request));
    }
}
