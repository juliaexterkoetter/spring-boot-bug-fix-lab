package com.example.bugfixlab.service;

import com.example.bugfixlab.dto.OrderItemRequest;
import com.example.bugfixlab.dto.OrderRequest;
import com.example.bugfixlab.entity.Customer;
import com.example.bugfixlab.entity.Order;
import com.example.bugfixlab.entity.Product;
import com.example.bugfixlab.exception.ResourceNotFoundException;
import com.example.bugfixlab.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository repository;
    @Mock CustomerService customers;
    @Mock ProductService products;
    @InjectMocks OrderService service;

    @Test
    void calculatesTotalsUsingProductPrices() {
        when(customers.requireById(1L)).thenReturn(new Customer("Alex", "alex@example.com"));
        Product keyboard = new Product("Keyboard", new BigDecimal("19.99"));
        keyboard.setStock(3);
        Product cable = new Product("Cable", new BigDecimal("0.10"));
        cable.setStock(2);
        when(products.requireById(2L)).thenReturn(keyboard);
        when(products.requireById(3L)).thenReturn(cable);
        when(repository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new OrderRequest(1L,
                List.of(new OrderItemRequest(2L, 3), new OrderItemRequest(3L, 2))));

        assertThat(response.total()).isEqualByComparingTo("60.17");
        assertThat(response.items().getFirst().subtotal()).isEqualByComparingTo("59.97");
        verify(repository).saveAndFlush(any(Order.class));
    }

    @Test
    void doesNotSaveWhenProductDoesNotExist() {
        when(customers.requireById(1L)).thenReturn(new Customer("Alex", "alex@example.com"));
        when(products.requireById(99L)).thenThrow(new ResourceNotFoundException("Product not found: 99"));
        assertThatThrownBy(() -> service.create(new OrderRequest(1L, List.of(new OrderItemRequest(99L, 1)))))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void reportsMissingOrder() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found: 99");
    }

    @Test
    void keepsPriceSnapshotWhenProductPriceChanges() {
        Product product = new Product("Keyboard", new BigDecimal("19.99"));
        Order order = new Order(new Customer("Alex", "alex@example.com"));
        order.addItem(product, 2);
        product.setPrice(new BigDecimal("29.99"));
        assertThat(order.getTotal()).isEqualByComparingTo("39.98");
        assertThat(order.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("19.99");
    }
}
