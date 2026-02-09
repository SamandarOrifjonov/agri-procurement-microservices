package com.agrifood.supplier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Supplier Service - Manages supplier information and product catalog
 * Independent microservice for supplier operations
 */
@SpringBootApplication(scanBasePackages = {"com.agrifood.supplier", "com.agrifood.common"})
public class SupplierServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupplierServiceApplication.class, args);
    }
}
