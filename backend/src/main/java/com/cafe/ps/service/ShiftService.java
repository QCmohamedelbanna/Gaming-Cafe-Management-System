package com.cafe.ps.service;

import com.cafe.ps.dto.CloseShiftRequest;
import com.cafe.ps.dto.OpenShiftRequest;
import com.cafe.ps.dto.ShiftReportResponse;
import com.cafe.ps.dto.ShiftResponse;
import com.cafe.ps.dto.ShiftTransactionResponse;
import com.cafe.ps.entity.AppUser;
import com.cafe.ps.entity.Payment;
import com.cafe.ps.entity.PaymentMethod;
import com.cafe.ps.entity.PaymentStatus;
import com.cafe.ps.entity.Role;
import com.cafe.ps.entity.Shift;
import com.cafe.ps.entity.ShiftStatus;
import com.cafe.ps.repository.PaymentRepository;
import com.cafe.ps.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final ShiftRepository shiftRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;

    @Transactional
    public ShiftResponse open(String username, OpenShiftRequest request) {
        AppUser user = userService.requireByUsername(username);
        if (shiftRepository.findByUserIdAndStatus(user.getId(), ShiftStatus.OPEN).isPresent()) {
            throw new IllegalStateException("You already have an open shift");
        }

        Shift shift = Shift.builder()
                .user(user)
                .openedAt(LocalDateTime.now())
                .status(ShiftStatus.OPEN)
                .openingCash(money(request.openingCash()))
                .build();
        return toResponse(shiftRepository.save(shift));
    }

    @Transactional
    public ShiftResponse close(String username, CloseShiftRequest request) {
        AppUser user = userService.requireByUsername(username);
        Shift shift = shiftRepository.findByUserIdAndStatus(user.getId(), ShiftStatus.OPEN)
                .orElseThrow(() -> new IllegalStateException("No open shift found"));

        ShiftTotals totals = summarize(shift);
        BigDecimal expected = money(nz(shift.getOpeningCash()).add(totals.cashCollected).subtract(totals.cashRefunds));
        BigDecimal actual = money(request.actualCash());
        shift.setExpectedCash(expected);
        shift.setActualCash(actual);
        shift.setCashDifference(money(actual.subtract(expected)));
        shift.setClosedAt(LocalDateTime.now());
        shift.setClosingNote(normalizeNote(request.note()));
        shift.setStatus(ShiftStatus.CLOSED);
        return toResponse(shiftRepository.save(shift));
    }

    @Transactional(readOnly = true)
    public Optional<ShiftResponse> current(String username) {
        AppUser user = userService.requireByUsername(username);
        return shiftRepository.findByUserIdAndStatus(user.getId(), ShiftStatus.OPEN)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> mine(String username) {
        AppUser user = userService.requireByUsername(username);
        return shiftRepository.findByUserIdOrderByOpenedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> all() {
        return shiftRepository.findAllByOrderByOpenedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShiftReportResponse report(Long id, String username, Role role) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found"));
        if (role == Role.CASHIER && !shift.getUser().getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("Cashiers can only view their own shifts");
        }

        ShiftTotals totals = summarize(shift);
        List<ShiftTransactionResponse> transactions = paymentRepository.findByShiftIdOrderByPaidAtAsc(id)
                .stream()
                .map(ShiftService::toTransaction)
                .toList();

        return new ShiftReportResponse(
                toResponse(shift),
                money(totals.cashCollected),
                money(totals.cardCollected),
                money(totals.mobileWalletCollected),
                money(totals.totalCollected),
                money(totals.refunds),
                money(totals.totalCollected.subtract(totals.refunds)),
                totals.transactionCount,
                transactions
        );
    }

    @Transactional(readOnly = true)
    public Optional<Shift> openShiftFor(AppUser user) {
        return shiftRepository.findByUserIdAndStatus(user.getId(), ShiftStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public Shift requireOpenShiftForCashier(AppUser user) {
        return openShiftFor(user)
                .orElseThrow(() -> new IllegalStateException("Open a cashier shift before taking payments"));
    }

    private ShiftResponse toResponse(Shift shift) {
        ShiftTotals totals = summarize(shift);
        BigDecimal expected = shift.getStatus() == ShiftStatus.CLOSED && shift.getExpectedCash() != null
                ? money(shift.getExpectedCash())
                : money(nz(shift.getOpeningCash()).add(totals.cashCollected).subtract(totals.cashRefunds));
        BigDecimal actual = shift.getActualCash() == null ? null : money(shift.getActualCash());
        BigDecimal difference = actual == null ? null : money(actual.subtract(expected));

        return new ShiftResponse(
                shift.getId(),
                shift.getUser().getId(),
                shift.getUser().getUsername(),
                shift.getUser().getDisplayName(),
                shift.getUser().getRole(),
                shift.getOpenedAt(),
                shift.getClosedAt(),
                shift.getStatus(),
                money(shift.getOpeningCash()),
                expected,
                actual,
                difference,
                shift.getClosingNote()
        );
    }

    private ShiftTotals summarize(Shift shift) {
        List<Payment> payments = paymentRepository.findByShiftIdOrderByPaidAtAsc(shift.getId());
        ShiftTotals totals = new ShiftTotals();
        for (Payment payment : payments) {
            BigDecimal amount = nz(payment.getAmount());
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                totals.transactionCount++;
                totals.totalCollected = totals.totalCollected.add(amount);
                addByMethod(totals, payment.getMethod(), amount);
            } else if (payment.getStatus() == PaymentStatus.REFUNDED) {
                totals.refunds = totals.refunds.add(amount);
                if (payment.getMethod() == PaymentMethod.CASH) {
                    totals.cashRefunds = totals.cashRefunds.add(amount);
                }
            }
        }
        return totals;
    }

    private static void addByMethod(ShiftTotals totals, PaymentMethod method, BigDecimal amount) {
        if (method == PaymentMethod.CASH) totals.cashCollected = totals.cashCollected.add(amount);
        if (method == PaymentMethod.CARD) totals.cardCollected = totals.cardCollected.add(amount);
        if (method == PaymentMethod.MOBILE_WALLET) totals.mobileWalletCollected = totals.mobileWalletCollected.add(amount);
    }

    private static ShiftTransactionResponse toTransaction(Payment payment) {
        return new ShiftTransactionResponse(
                payment.getId(),
                payment.getBill() == null ? null : payment.getBill().getId(),
                payment.getBill() == null ? null : payment.getBill().getBillNumber(),
                payment.getMethod(),
                money(payment.getAmount()),
                money(payment.getAmountTendered()),
                money(payment.getChangeAmount()),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getCashier()
        );
    }

    private static String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return nz(value).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private static final class ShiftTotals {
        private BigDecimal cashCollected = BigDecimal.ZERO;
        private BigDecimal cardCollected = BigDecimal.ZERO;
        private BigDecimal mobileWalletCollected = BigDecimal.ZERO;
        private BigDecimal totalCollected = BigDecimal.ZERO;
        private BigDecimal refunds = BigDecimal.ZERO;
        private BigDecimal cashRefunds = BigDecimal.ZERO;
        private long transactionCount;
    }
}
