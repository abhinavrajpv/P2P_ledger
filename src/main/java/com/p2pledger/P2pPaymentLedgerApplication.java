package com.p2pledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class P2pPaymentLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(P2pPaymentLedgerApplication.class, args);
    }
}
