package com.insurance.claims.service;

import com.insurance.claims.exception.ResourceNotFoundException;
import com.insurance.claims.exception.ValidationException;
import com.insurance.claims.model.Customer;
import com.insurance.claims.repository.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    public Customer createCustomer(Customer customer) {
        customers.findByEmail(customer.getEmail()).ifPresent(existing -> {
            throw new ValidationException(
                    "A customer with email " + customer.getEmail() + " already exists");
        });
        return customers.save(customer);
    }

    @Transactional(readOnly = true)
    public List<Customer> listCustomers() {
        return customers.findAll();
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(Long id) {
        return customers.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }
}
