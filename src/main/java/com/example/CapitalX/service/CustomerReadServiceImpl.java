package com.example.CapitalX.service;

import com.example.CapitalX.beans.Customer;
import com.example.CapitalX.dto.CustomerOverviewDTO;
import com.example.CapitalX.exceptions.CustomerNotFoundException;
import com.example.CapitalX.mapper.CustomerMapper;
import com.example.CapitalX.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerReadServiceImpl implements CustomerReadService {
    private final CustomerRepository customerRepository;

    public CustomerReadServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public List<CustomerOverviewDTO> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        return customers.stream()
            .map(CustomerMapper::convertBeanToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public CustomerOverviewDTO getCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));
        return CustomerMapper.convertBeanToDTO(customer);
    }

    @Override
    public CustomerOverviewDTO getCustomerByCode(String customerCode) {
        Customer customer = customerRepository.findByCustomerCode(customerCode)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found with code: " + customerCode));
        return CustomerMapper.convertBeanToDTO(customer);
    }

    @Override
    public boolean customerExists(Long customerId) {
        return customerRepository.existsById(customerId);
    }
}
