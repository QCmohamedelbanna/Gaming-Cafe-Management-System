package com.cafe.ps.controller;
import com.cafe.ps.entity.Pricing;
import com.cafe.ps.repository.PricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/pricing") @RequiredArgsConstructor @CrossOrigin(origins = "http://localhost:5173")
public class PricingController {
    private final PricingRepository repository;
    @GetMapping public List<Pricing> all() { return repository.findAll(); }
}
