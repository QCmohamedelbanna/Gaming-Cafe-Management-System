package com.cafe.ps.dto;

import com.cafe.ps.entity.Customer;

public record CustomerResponse(
        Long id,
        String name,
        String phone,
        String email,
        String notes
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getNotes()
        );
    }
}
