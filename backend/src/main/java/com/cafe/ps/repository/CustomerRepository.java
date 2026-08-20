package com.cafe.ps.repository;

import com.cafe.ps.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    @Query("""
            select c from Customer c
            where lower(c.name) like lower(concat('%', :search, '%'))
               or c.phone like concat('%', :search, '%')
            order by c.name asc
            """)
    List<Customer> search(@Param("search") String search);
}
