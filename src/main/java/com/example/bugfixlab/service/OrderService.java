package com.example.bugfixlab.service;

import com.example.bugfixlab.dto.*;
import com.example.bugfixlab.entity.Order;
import com.example.bugfixlab.entity.Product;
import com.example.bugfixlab.exception.ConflictException;
import com.example.bugfixlab.exception.ResourceNotFoundException;
import com.example.bugfixlab.repository.OrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository repository;
    private final CustomerService customers;
    private final ProductService products;

    public OrderService(OrderRepository repository, CustomerService customers, ProductService products) {
        this.repository = repository;
        this.customers = customers;
        this.products = products;
    }

    public List<OrderResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    public OrderResponse findById(Long id) {
        return toResponse(requireById(id));
    }

    private Order requireById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Order order = new Order(customers.requireById(request.customerId()));
        Map<Long, Integer> requestedQuantities = new HashMap<>();
        for (OrderItemRequest item : request.items()) {
            Product product = products.requireById(item.productId());
            int requestedQuantity = requestedQuantities.merge(item.productId(), item.quantity(), Integer::sum);
            if (requestedQuantity > product.getStock()) {
                throw new ConflictException("Insufficient stock for product: " + item.productId());
            }
            order.addItem(product, item.quantity());
        }
        return toResponse(repository.saveAndFlush(order));
    }

    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        Order order = requireById(id);
        order.setCustomer(customers.requireById(request.customerId()));
        replaceItems(order, request);
        return toResponse(repository.saveAndFlush(order));
    }

    private void replaceItems(Order order, OrderRequest request) {
        order.clearItems();
        for (OrderItemRequest item : request.items()) {
            order.addItem(products.requireById(item.productId()), item.quantity());
        }
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(requireById(id));
        repository.flush();
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(), order.getCustomer().getId(), order.getCreatedAt(),
                order.getItems().stream().map(item -> new OrderItemResponse(item.getId(),
                        item.getProduct().getId(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal())).toList(),
                order.getTotal());
    }
}
