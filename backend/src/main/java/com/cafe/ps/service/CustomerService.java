package com.cafe.ps.service;

import com.cafe.ps.dto.CustomerRequest;
import com.cafe.ps.entity.Customer;
import com.cafe.ps.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;

    @Transactional(readOnly = true)
    public List<Customer> search(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return repository.findAll();
        }
        return repository.search(normalized);
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        String phone = request.phone().trim();
        if (repository.findByPhone(phone).isPresent()) {
            throw new IllegalStateException("A customer with this phone number already exists");
        }

        return repository.save(Customer.builder()
                .name(request.name().trim())
                .phone(phone)
                .email(normalizeOptional(request.email()))
                .notes(normalizeOptional(request.notes()))
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = get(id);
        String phone = request.phone().trim();

        if (repository.existsByPhoneAndIdNot(phone, id)) {
            throw new IllegalStateException("A customer with this phone number already exists");
        }

        customer.setName(request.name().trim());
        customer.setPhone(phone);
        customer.setEmail(normalizeOptional(request.email()));
        customer.setNotes(normalizeOptional(request.notes()));
        return repository.save(customer);
    }

    /** Reuses an existing customer by phone, or creates a lightweight one for a new reservation. */
    @Transactional
    public Customer findOrCreate(String name, String phone) {
        String normalizedPhone = phone.trim();
        return repository.findByPhone(normalizedPhone)
                .orElseGet(() -> repository.save(Customer.builder()
                        .name(name.trim())
                        .phone(normalizedPhone)
                        .createdAt(LocalDateTime.now())
                        .build()));
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
