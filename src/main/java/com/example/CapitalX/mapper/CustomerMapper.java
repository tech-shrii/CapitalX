package com.example.CapitalX.mapper;

import com.example.CapitalX.beans.Customer;
import com.example.CapitalX.dto.CustomerOverviewDTO;

public class CustomerMapper {

    public static CustomerOverviewDTO convertBeanToDTO(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerOverviewDTO(
            customer.getCustomerId(),
            customer.getCustomerCode(),
            customer.getCustomerName(),
            customer.getCreatedAt()
        );
    }
}
