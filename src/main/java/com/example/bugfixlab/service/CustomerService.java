package com.example.bugfixlab.service;

import com.example.bugfixlab.dto.CustomerRequest;
import com.example.bugfixlab.dto.CustomerResponse;
import com.example.bugfixlab.entity.Customer;
import com.example.bugfixlab.exception.ResourceNotFoundException;
import com.example.bugfixlab.exception.ConflictException;
import com.example.bugfixlab.repository.CustomerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerService {
    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) { this.repository = repository; }

    public List<CustomerResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    public CustomerResponse findById(Long id) { return toResponse(requireById(id)); }

    public Customer requireById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) { return save(null, request); }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) { return save(id, request); }

    private CustomerResponse save(Long id, CustomerRequest request) {
        Customer entity = id == null ? new Customer(request.name(), request.email()) : requireById(id);
        String email = request.email().strip().toLowerCase(java.util.Locale.ROOT);
        boolean duplicate = id == null ? repository.existsByEmailIgnoreCase(email)
                : repository.existsByEmailIgnoreCaseAndIdNot(email, id);
        if (duplicate) throw new ConflictException("Email is already in use");
        entity.setName(request.name().strip());
        entity.setEmail(email);
        return toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(requireById(id));
        repository.flush();
    }

    private CustomerResponse toResponse(Customer entity) {
        return new CustomerResponse(entity.getId(), entity.getName(), entity.getEmail());
    }
}
