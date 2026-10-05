package com.example.bugfixlab.service;

import com.example.bugfixlab.dto.ProductRequest;
import com.example.bugfixlab.dto.ProductResponse;
import com.example.bugfixlab.entity.Product;
import com.example.bugfixlab.exception.ResourceNotFoundException;
import com.example.bugfixlab.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) { this.repository = repository; }

    public List<ProductResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    public ProductResponse findById(Long id) { return toResponse(requireById(id)); }

    public Product requireById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) { return save(null, request); }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) { return save(id, request); }

    private ProductResponse save(Long id, ProductRequest request) {
        Product entity = id == null ? new Product(request.name(), request.price()) : requireById(id);
        entity.setName(request.name().strip());
        entity.setPrice(request.price());
        return toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(requireById(id));
        repository.flush();
    }

    private ProductResponse toResponse(Product entity) {
        return new ProductResponse(entity.getId(), entity.getName(), entity.getPrice());
    }
}
