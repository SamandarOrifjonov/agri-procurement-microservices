package com.agrifood.contract;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Contract Service - Manages contracts between buyers and suppliers
 * Demonstrates Saga pattern for distributed transaction reliability
 */
@SpringBootApplication(scanBasePackages = {"com.agrifood.contract", "com.agrifood.common"})
public class ContractServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContractServiceApplication.class, args);
    }
}
