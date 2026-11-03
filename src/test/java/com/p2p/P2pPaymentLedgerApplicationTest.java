package com.p2p;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import com.p2pledger.P2pPaymentLedgerApplication;

public class P2pPaymentLedgerApplicationTest {

    @Test
    void constructorTest() {
        P2pPaymentLedgerApplication application =
                new P2pPaymentLedgerApplication();

        assertNotNull(application);
    }

    @Test
    void mainTest() {
        String[] args = {"--spring.main.web-application-type=none"};

        try (MockedStatic<SpringApplication> mocked =
                     mockStatic(SpringApplication.class)) {

            P2pPaymentLedgerApplication.main(args);

            mocked.verify(() ->
                    SpringApplication.run(
                            P2pPaymentLedgerApplication.class,
                            args
                    )
            );
        }
    }
}