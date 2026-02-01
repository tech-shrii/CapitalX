package com.example.CapitalX.service;

import com.example.CapitalX.dto.CustomerOverviewDTO;
import java.util.List;

public interface CustomerReadService {
    List<CustomerOverviewDTO> getAllCustomers();
    CustomerOverviewDTO getCustomerById(Long customerId);
    CustomerOverviewDTO getCustomerByCode(String customerCode);
    boolean customerExists(Long customerId);
}
