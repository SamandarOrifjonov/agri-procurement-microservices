package com.agrifood.procurement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Procurement Service - Manages buyer requirements and procurement lifecycle
 * Part of microservices architecture for scalability
 */
@SpringBootApplication(scanBasePackages = {"com.agrifood.procurement", "com.agrifood.common"})
public class ProcurementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcurementServiceApplication.class, args);
    }
}
