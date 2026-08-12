package com.cafe.ps.repository;

import com.cafe.ps.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    List<Product> findAllByActiveTrueAndDeletedFalseOrderByNameAsc();

    List<Product> findAllByDeletedFalseOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );
}
