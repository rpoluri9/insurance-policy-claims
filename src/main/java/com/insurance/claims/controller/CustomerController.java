package com.insurance.claims.controller;

import com.insurance.claims.model.Customer;
import com.insurance.claims.model.Requests.CreateCustomerRequest;
import com.insurance.claims.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = new Customer(request.firstName(), request.lastName(), request.email(),
                request.phone(), request.dateOfBirth(), request.address());
        return ResponseEntity.status(HttpStatus.CREATED).body(customers.createCustomer(customer));
    }

    @GetMapping
    public List<Customer> listCustomers() {
        return customers.listCustomers();
    }

    @GetMapping("/{id}")
    public Customer getCustomer(@PathVariable Long id) {
        return customers.getCustomer(id);
    }
}
