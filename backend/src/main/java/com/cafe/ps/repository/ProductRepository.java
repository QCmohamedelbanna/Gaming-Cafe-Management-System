package com.cafe.ps.repository;

import com.cafe.ps.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    List<Product> findAllByActiveTrueOrderByNameAsc();
}