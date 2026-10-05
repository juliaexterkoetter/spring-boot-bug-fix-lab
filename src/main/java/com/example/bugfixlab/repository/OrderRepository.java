package com.example.bugfixlab.repository;

import com.example.bugfixlab.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
