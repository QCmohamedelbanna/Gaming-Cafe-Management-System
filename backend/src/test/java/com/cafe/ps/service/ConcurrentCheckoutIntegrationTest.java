package com.cafe.ps.service;

import com.cafe.ps.AbstractMySQLIntegrationTest;
import com.cafe.ps.dto.CheckoutResult;
import com.cafe.ps.entity.BillStatus;
import com.cafe.ps.entity.BillingUnit;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.DeviceType;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.entity.PaymentMethod;
import com.cafe.ps.entity.SessionStatus;
import com.cafe.ps.entity.SessionType;
import com.cafe.ps.repository.BillRepository;
import com.cafe.ps.repository.CafeOrderRepository;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.GameSessionRepository;
import com.cafe.ps.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two threads attempt to check out the same active session at the same
 * time (e.g. a double-tap on the checkout button, or two cashiers on
 * different terminals). BillingService#checkoutSession locks the session
 * row (GameSessionRepository#findByIdForUpdate) for exactly this reason;
 * this test exercises real MySQL row locking, which the previous
 * single-connection SQLite test setup could not.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false"
        }
)
class ConcurrentCheckoutIntegrationTest extends AbstractMySQLIntegrationTest {

    private static final BigDecimal HOURLY_RATE = new BigDecimal("40.00");

    @Autowired
    private BillingService billingService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private CafeOrderRepository orderRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
        billRepository.deleteAll();
        orderRepository.deleteAll();
        sessionRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @RepeatedTest(3)
    void onlyOneOfTwoSimultaneousCheckoutsRecordsAPayment() throws InterruptedException {
        GameSession session = saveActiveSession();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<CheckoutResult>> futures = List.of(
                    pool.submit(() -> attemptCheckout(session.getId(), ready, go)),
                    pool.submit(() -> attemptCheckout(session.getId(), ready, go))
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            int succeeded = 0;
            for (var future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                    succeeded++;
                } catch (Exception ignoredRaceLoser) {
                    // The losing thread may see "Session cannot be billed" or a
                    // similar state error once the winner has already completed
                    // the session; that is the expected, safe outcome.
                }
            }

            assertThat(succeeded).isGreaterThanOrEqualTo(1);
        } finally {
            pool.shutdownNow();
        }

        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(billRepository.count()).isEqualTo(1);
        assertThat(billRepository.findAll().get(0).getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getStatus())
                .isEqualTo(SessionStatus.COMPLETED);
    }

    private CheckoutResult attemptCheckout(Long sessionId, CountDownLatch ready, CountDownLatch go) throws Exception {
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        return billingService.checkoutSession(sessionId, PaymentMethod.CASH, new BigDecimal("40.00"), "Admin");
    }

    private GameSession saveActiveSession() {
        Device device = deviceRepository.save(Device.builder()
                .name("CONCURRENT-TEST-" + UUID.randomUUID())
                .type(DeviceType.PS4)
                .status(DeviceStatus.PLAYING)
                .active(true)
                .deleted(false)
                .build());
        return sessionRepository.save(GameSession.builder()
                .device(device)
                .startTime(LocalDateTime.now().minusHours(1))
                .hourlyRateSnapshot(HOURLY_RATE)
                .unitPriceSnapshot(HOURLY_RATE)
                .sessionType(SessionType.SINGLE)
                .billingUnit(BillingUnit.HOUR)
                .status(SessionStatus.ACTIVE)
                .build());
    }
}
