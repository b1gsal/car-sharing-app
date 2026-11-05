package com.github.b1gsal.carsharingservice.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.b1gsal.carsharingservice.dto.payment.PaymentRequestDto;
import com.github.b1gsal.carsharingservice.dto.payment.PaymentResponseDto;
import com.github.b1gsal.carsharingservice.exception.EntityNotFoundException;
import com.github.b1gsal.carsharingservice.exception.PaymentException;
import com.github.b1gsal.carsharingservice.mapper.PaymentMapper;
import com.github.b1gsal.carsharingservice.model.Car;
import com.github.b1gsal.carsharingservice.model.Payment;
import com.github.b1gsal.carsharingservice.model.Rental;
import com.github.b1gsal.carsharingservice.model.Role;
import com.github.b1gsal.carsharingservice.model.RoleName;
import com.github.b1gsal.carsharingservice.model.User;
import com.github.b1gsal.carsharingservice.repository.payment.PaymentRepository;
import com.github.b1gsal.carsharingservice.repository.rental.RentalRepository;
import com.github.b1gsal.carsharingservice.service.notification.NotificationService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RentalRepository rentalRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User customerUser;
    private User managerUser;
    private Rental rental;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "key", "sk_test_mock_key");
        ReflectionTestUtils.setField(paymentService, "fineMultiplier", new BigDecimal("2"));
        ReflectionTestUtils.setField(paymentService, "domainUrl", "http://localhost:8080");

        Role customerRole = new Role();
        customerRole.setId(1L);
        customerRole.setName(RoleName.ROLE_CUSTOMER);

        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName(RoleName.ROLE_MANAGER);

        customerUser = new User();
        customerUser.setId(10L);
        customerUser.setEmail("customer@example.com");
        customerUser.setRoles(Set.of(customerRole));

        managerUser = new User();
        managerUser.setId(1L);
        managerUser.setEmail("manager@example.com");
        managerUser.setRoles(Set.of(managerRole));

        Car car = new Car();
        car.setId(5L);
        car.setBrand("Skoda");
        car.setModel("Octavia");
        car.setDailyFee(new BigDecimal("30.00"));

        rental = new Rental();
        rental.setId(100L);
        rental.setUser(customerUser);
        rental.setCar(car);
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(LocalDate.now().plusDays(3));
    }

    @Test
    @DisplayName("Create payment - valid PAYMENT type - should create Stripe session and return DTO")
    void createPayment_validPaymentType_shouldCreatePayment() {
        Authentication authentication = mock(Authentication.class);
        PaymentRequestDto requestDto = new PaymentRequestDto(100L, Payment.Type.PAYMENT);

        Payment savedPayment = new Payment();
        savedPayment.setId(1L);
        savedPayment.setRental(rental);
        savedPayment.setSessionId("sess_123");
        savedPayment.setSessionUrl("http://stripe.com/session");
        savedPayment.setAmountToPay(new BigDecimal("90.00"));
        savedPayment.setStatus(Payment.Status.PENDING);
        savedPayment.setType(Payment.Type.PAYMENT);

        PaymentResponseDto expected = new PaymentResponseDto(
                1L, Payment.Status.PENDING, Payment.Type.PAYMENT, 100L,
                "http://stripe.com/session", "sess_123", new BigDecimal("90.00")
        );

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(100L, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.EXPIRED))
                .thenReturn(Optional.empty());
        when(rentalRepository.findById(100L)).thenReturn(Optional.of(rental));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(paymentMapper.toDto(savedPayment)).thenReturn(expected);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session stripeSession = mock(Session.class);
            when(stripeSession.getId()).thenReturn("sess_123");
            when(stripeSession.getUrl()).thenReturn("http://stripe.com/session");
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(stripeSession);

            PaymentResponseDto actual = paymentService.createPayment(requestDto, authentication);

            assertThat(actual).isEqualTo(expected);
        }

        verify(paymentRepository, times(1)).existsByRentalIdAndTypeAndStatus(100L, Payment.Type.PAYMENT, Payment.Status.PAID);
        verify(paymentRepository, times(1)).findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.PENDING);
        verify(paymentRepository, times(1)).findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.EXPIRED);
        verify(rentalRepository, times(1)).findById(100L);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentMapper, times(1)).toDto(savedPayment);
    }

    @Test
    @DisplayName("Create payment - FINE type with overdue days - should calculate fine with multiplier")
    void createPayment_validFineType_shouldCalculateFine() {
        Authentication authentication = mock(Authentication.class);
        PaymentRequestDto requestDto = new PaymentRequestDto(100L, Payment.Type.FINE);

        rental.setActualReturnDate(rental.getReturnDate().plusDays(2)); // 2 overdue days * $30 * 2 multiplier = $120

        Payment savedPayment = new Payment();
        savedPayment.setId(2L);
        savedPayment.setAmountToPay(new BigDecimal("120.00"));

        PaymentResponseDto expected = new PaymentResponseDto(
                2L, Payment.Status.PENDING, Payment.Type.FINE, 100L,
                "http://stripe.com/fine", "sess_fine", new BigDecimal("120.00")
        );

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(100L, Payment.Type.FINE, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.FINE, Payment.Status.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.FINE, Payment.Status.EXPIRED))
                .thenReturn(Optional.empty());
        when(rentalRepository.findById(100L)).thenReturn(Optional.of(rental));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(paymentMapper.toDto(savedPayment)).thenReturn(expected);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session stripeSession = mock(Session.class);
            when(stripeSession.getId()).thenReturn("sess_fine");
            when(stripeSession.getUrl()).thenReturn("http://stripe.com/fine");
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(stripeSession);

            PaymentResponseDto actual = paymentService.createPayment(requestDto, authentication);

            assertThat(actual).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("Create payment - FINE type for unreturned car - should throw PaymentException")
    void createPayment_fineUnreturnedRental_shouldThrowPaymentException() {
        Authentication authentication = mock(Authentication.class);
        PaymentRequestDto requestDto = new PaymentRequestDto(100L, Payment.Type.FINE);

        rental.setActualReturnDate(null);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(100L, Payment.Type.FINE, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.FINE, Payment.Status.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.FINE, Payment.Status.EXPIRED))
                .thenReturn(Optional.empty());
        when(rentalRepository.findById(100L)).thenReturn(Optional.of(rental));

        assertThrows(PaymentException.class, () -> paymentService.createPayment(requestDto, authentication));
    }

    @Test
    @DisplayName("Create payment - FINE type when not overdue - should throw PaymentException")
    void createPayment_fineNotOverdue_shouldThrowPaymentException() {
        Authentication authentication = mock(Authentication.class);
        PaymentRequestDto requestDto = new PaymentRequestDto(100L, Payment.Type.FINE);

        rental.setActualReturnDate(rental.getReturnDate()); // returned on time

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(100L, Payment.Type.FINE, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.FINE, Payment.Status.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.FINE, Payment.Status.EXPIRED))
                .thenReturn(Optional.empty());
        when(rentalRepository.findById(100L)).thenReturn(Optional.of(rental));

        assertThrows(PaymentException.class, () -> paymentService.createPayment(requestDto, authentication));
    }

    @Test
    @DisplayName("Create payment - already completed - should throw PaymentException")
    void createPayment_alreadyPaid_shouldThrowPaymentException() {
        Authentication authentication = mock(Authentication.class);
        PaymentRequestDto requestDto = new PaymentRequestDto(100L, Payment.Type.PAYMENT);

        when(paymentRepository.existsByRentalIdAndTypeAndStatus(100L, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(true);

        assertThrows(PaymentException.class, () -> paymentService.createPayment(requestDto, authentication));

        verify(paymentRepository, times(1)).existsByRentalIdAndTypeAndStatus(100L, Payment.Type.PAYMENT, Payment.Status.PAID);
        verifyNoMoreInteractions(paymentRepository);
        verifyNoInteractions(rentalRepository, paymentMapper, notificationService);
    }

    @Test
    @DisplayName("Create payment - existing pending payment exists - should return pending payment DTO")
    void createPayment_pendingPaymentExists_shouldReturnPendingPayment() {
        Authentication authentication = mock(Authentication.class);
        PaymentRequestDto requestDto = new PaymentRequestDto(100L, Payment.Type.PAYMENT);
        Payment pendingPayment = new Payment();

        PaymentResponseDto expected = new PaymentResponseDto(
                1L, Payment.Status.PENDING, Payment.Type.PAYMENT, 100L, "url", "sess", new BigDecimal("90.00")
        );

        when(paymentRepository.existsByRentalIdAndTypeAndStatus(100L, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.PENDING))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentMapper.toDto(pendingPayment)).thenReturn(expected);

        PaymentResponseDto actual = paymentService.createPayment(requestDto, authentication);

        assertThat(actual).isEqualTo(expected);

        verify(paymentRepository, times(1)).existsByRentalIdAndTypeAndStatus(100L, Payment.Type.PAYMENT, Payment.Status.PAID);
        verify(paymentRepository, times(1)).findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.PENDING);
        verify(paymentMapper, times(1)).toDto(pendingPayment);
        verifyNoMoreInteractions(paymentRepository, paymentMapper);
    }

    @Test
    @DisplayName("Create payment - no permission for rental - should throw PaymentException")
    void createPayment_noPermission_shouldThrowPaymentException() {
        Authentication authentication = mock(Authentication.class);
        PaymentRequestDto requestDto = new PaymentRequestDto(100L, Payment.Type.PAYMENT);

        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setRoles(customerUser.getRoles());
        rental.setUser(otherUser);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(100L, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.EXPIRED))
                .thenReturn(Optional.empty());
        when(rentalRepository.findById(100L)).thenReturn(Optional.of(rental));

        assertThrows(PaymentException.class, () -> paymentService.createPayment(requestDto, authentication));
    }

    @Test
    @DisplayName("Process successful payment - valid session paid - should set PAID and send notification")
    void processSuccessfulPayment_validPaidSession_shouldMarkPaidAndNotify() {
        String sessionId = "sess_123";
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setSessionId(sessionId);
        payment.setStatus(Payment.Status.PENDING);

        Payment savedPayment = new Payment();
        savedPayment.setId(1L);
        savedPayment.setStatus(Payment.Status.PAID);

        PaymentResponseDto expected = new PaymentResponseDto(
                1L, Payment.Status.PAID, Payment.Type.PAYMENT, 100L, "url", sessionId, new BigDecimal("90.00")
        );

        when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(savedPayment);
        when(paymentMapper.toDto(savedPayment)).thenReturn(expected);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session stripeSession = mock(Session.class);
            when(stripeSession.getPaymentStatus()).thenReturn("paid");
            mockedSession.when(() -> Session.retrieve(sessionId)).thenReturn(stripeSession);

            PaymentResponseDto actual = paymentService.processSuccessfulPayment(sessionId);

            assertThat(actual).isEqualTo(expected);
            assertThat(payment.getStatus()).isEqualTo(Payment.Status.PAID);
        }

        verify(paymentRepository, times(1)).findBySessionId(sessionId);
        verify(paymentRepository, times(1)).save(payment);
        verify(notificationService, times(1)).notifyPaymentSuccess(savedPayment);
        verify(paymentMapper, times(1)).toDto(savedPayment);
    }

    @Test
    @DisplayName("Process successful payment - session not paid on Stripe - should throw PaymentException")
    void processSuccessfulPayment_unpaidStripeSession_shouldThrowPaymentException() {
        String sessionId = "sess_123";
        Payment payment = new Payment();

        when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.of(payment));

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session stripeSession = mock(Session.class);
            when(stripeSession.getPaymentStatus()).thenReturn("unpaid");
            mockedSession.when(() -> Session.retrieve(sessionId)).thenReturn(stripeSession);

            assertThrows(PaymentException.class, () -> paymentService.processSuccessfulPayment(sessionId));
        }
    }

    @Test
    @DisplayName("Process successful payment - payment not found - should throw EntityNotFoundException")
    void processSuccessfulPayment_notFound_shouldThrowEntityNotFoundException() {
        String sessionId = "invalid_sess";
        when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> paymentService.processSuccessfulPayment(sessionId));
    }

    @Test
    @DisplayName("Get payments - as customer - should return customer payments page")
    void getPayments_asCustomer_shouldReturnCustomerPayments() {
        Authentication authentication = mock(Authentication.class);
        Pageable pageable = PageRequest.of(0, 10);
        Payment payment = new Payment();
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));
        PaymentResponseDto dto = new PaymentResponseDto(1L, Payment.Status.PAID, Payment.Type.PAYMENT, 100L, "url", "sess", new BigDecimal("90.00"));

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.findByRentalUserId(10L, pageable)).thenReturn(paymentPage);
        when(paymentMapper.toDto(payment)).thenReturn(dto);

        Page<PaymentResponseDto> actual = paymentService.getPayments(authentication, pageable, null);

        assertThat(actual.getContent()).containsExactly(dto);
        verify(paymentRepository, times(1)).findByRentalUserId(10L, pageable);
        verify(paymentMapper, times(1)).toDto(payment);
    }

    @Test
    @DisplayName("Get payments - as manager with specific userId - should return user payments")
    void getPayments_asManagerWithUserId_shouldReturnUserPayments() {
        Authentication authentication = mock(Authentication.class);
        Pageable pageable = PageRequest.of(0, 10);
        Long targetUserId = 22L;
        Payment payment = new Payment();
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));
        PaymentResponseDto dto = new PaymentResponseDto(1L, Payment.Status.PAID, Payment.Type.PAYMENT, 100L, "url", "sess", new BigDecimal("90.00"));

        when(authentication.getPrincipal()).thenReturn(managerUser);
        when(paymentRepository.findByRentalUserId(targetUserId, pageable)).thenReturn(paymentPage);
        when(paymentMapper.toDto(payment)).thenReturn(dto);

        Page<PaymentResponseDto> actual = paymentService.getPayments(authentication, pageable, targetUserId);

        assertThat(actual.getContent()).containsExactly(dto);
        verify(paymentRepository, times(1)).findByRentalUserId(targetUserId, pageable);
        verify(paymentMapper, times(1)).toDto(payment);
    }

    @Test
    @DisplayName("Get payments - as manager without userId - should return all payments")
    void getPayments_asManagerNoUserId_shouldReturnAllPayments() {
        Authentication authentication = mock(Authentication.class);
        Pageable pageable = PageRequest.of(0, 10);
        Payment payment = new Payment();
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));
        PaymentResponseDto dto = new PaymentResponseDto(1L, Payment.Status.PAID, Payment.Type.PAYMENT, 100L, "url", "sess", new BigDecimal("90.00"));

        when(authentication.getPrincipal()).thenReturn(managerUser);
        when(paymentRepository.findAll(pageable)).thenReturn(paymentPage);
        when(paymentMapper.toDto(payment)).thenReturn(dto);

        Page<PaymentResponseDto> actual = paymentService.getPayments(authentication, pageable, null);

        assertThat(actual.getContent()).containsExactly(dto);
        verify(paymentRepository, times(1)).findAll(pageable);
        verify(paymentMapper, times(1)).toDto(payment);
    }

    @Test
    @DisplayName("Renew payment - valid EXPIRED payment - should create new Stripe session and set PENDING")
    void renewPayment_validExpiredPayment_shouldRenewSession() {
        Authentication authentication = mock(Authentication.class);
        Long paymentId = 1L;

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setRental(rental);
        payment.setStatus(Payment.Status.EXPIRED);
        payment.setAmountToPay(new BigDecimal("90.00"));
        payment.setType(Payment.Type.PAYMENT);

        Payment updatedPayment = new Payment();
        updatedPayment.setId(paymentId);
        updatedPayment.setSessionId("new_sess");

        PaymentResponseDto expected = new PaymentResponseDto(
                paymentId, Payment.Status.PENDING, Payment.Type.PAYMENT, 100L, "new_url", "new_sess", new BigDecimal("90.00")
        );

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(updatedPayment);
        when(paymentMapper.toDto(updatedPayment)).thenReturn(expected);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session newStripeSession = mock(Session.class);
            when(newStripeSession.getId()).thenReturn("new_sess");
            when(newStripeSession.getUrl()).thenReturn("new_url");
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(newStripeSession);

            PaymentResponseDto actual = paymentService.renewPayment(paymentId, authentication);

            assertThat(actual).isEqualTo(expected);
            assertThat(payment.getStatus()).isEqualTo(Payment.Status.PENDING);
            assertThat(payment.getSessionId()).isEqualTo("new_sess");
        }

        verify(paymentRepository, times(1)).findById(paymentId);
        verify(paymentRepository, times(1)).save(payment);
        verify(paymentMapper, times(1)).toDto(updatedPayment);
    }

    @Test
    @DisplayName("Renew payment - status not EXPIRED - should throw PaymentException")
    void renewPayment_statusNotExpired_shouldThrowPaymentException() {
        Authentication authentication = mock(Authentication.class);
        Long paymentId = 1L;

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setRental(rental);
        payment.setStatus(Payment.Status.PENDING);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class, () -> paymentService.renewPayment(paymentId, authentication));
    }

    @Test
    @DisplayName("Check and update status - Stripe paid - should mark as PAID and notify")
    void checkAndUpdateStatus_paid_shouldMarkAsPaid() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setSessionId("sess_123");

        Payment savedPayment = new Payment();
        savedPayment.setId(1L);

        when(paymentRepository.save(payment)).thenReturn(savedPayment);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session stripeSession = mock(Session.class);
            when(stripeSession.getPaymentStatus()).thenReturn("paid");
            mockedSession.when(() -> Session.retrieve("sess_123")).thenReturn(stripeSession);

            paymentService.checkAndUpdateStatus(payment);

            assertThat(payment.getStatus()).isEqualTo(Payment.Status.PAID);
        }

        verify(paymentRepository, times(1)).save(payment);
        verify(notificationService, times(1)).notifyPaymentSuccess(savedPayment);
    }

    @Test
    @DisplayName("Check and update status - Stripe unpaid - should set EXPIRED status in DB")
    void checkAndUpdateStatus_unpaid_shouldSetExpired() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setSessionId("sess_123");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session stripeSession = mock(Session.class);
            when(stripeSession.getPaymentStatus()).thenReturn("unpaid");
            mockedSession.when(() -> Session.retrieve("sess_123")).thenReturn(stripeSession);

            paymentService.checkAndUpdateStatus(payment);
        }

        verify(paymentRepository, times(1)).updateStatus(1L, Payment.Status.EXPIRED);
    }

    @Test
    @DisplayName("Create payment - same day rental (days < 1) - should charge for minimum 1 day")
    void createPayment_sameDayRental_shouldChargeForOneDay() {
        Authentication authentication = mock(Authentication.class);
        Rental sameDayRental = new Rental();
        sameDayRental.setId(200L);
        sameDayRental.setUser(customerUser);
        sameDayRental.setCar(rental.getCar());
        sameDayRental.setRentalDate(LocalDate.now());
        sameDayRental.setReturnDate(LocalDate.now()); // 0 днів різниці

        PaymentRequestDto requestDto = new PaymentRequestDto(200L, Payment.Type.PAYMENT);

        Payment savedPayment = new Payment();
        savedPayment.setId(3L);
        savedPayment.setAmountToPay(new BigDecimal("30.00")); // $30 * 1 day

        PaymentResponseDto expected = new PaymentResponseDto(
                3L, Payment.Status.PENDING, Payment.Type.PAYMENT, 200L,
                "http://stripe.com/session", "sess_sameday", new BigDecimal("30.00")
        );

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(200L, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(200L, Payment.Type.PAYMENT, Payment.Status.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(200L, Payment.Type.PAYMENT, Payment.Status.EXPIRED))
                .thenReturn(Optional.empty());
        when(rentalRepository.findById(200L)).thenReturn(Optional.of(sameDayRental));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(paymentMapper.toDto(savedPayment)).thenReturn(expected);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session stripeSession = mock(Session.class);
            when(stripeSession.getId()).thenReturn("sess_sameday");
            when(stripeSession.getUrl()).thenReturn("http://stripe.com/session");
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(stripeSession);

            PaymentResponseDto actual = paymentService.createPayment(requestDto, authentication);

            assertThat(actual.amountToPay()).isEqualTo(new BigDecimal("30.00"));
        }
    }

    @Test
    @DisplayName("Create payment - expired payment exists - should invoke renewPayment automatically")
    void createPayment_expiredPaymentExists_shouldRenewPayment() {
        Authentication authentication = mock(Authentication.class);
        PaymentRequestDto requestDto = new PaymentRequestDto(100L, Payment.Type.PAYMENT);

        Payment expiredPayment = new Payment();
        expiredPayment.setId(55L);
        expiredPayment.setRental(rental);
        expiredPayment.setStatus(Payment.Status.EXPIRED);
        expiredPayment.setAmountToPay(new BigDecimal("90.00"));
        expiredPayment.setType(Payment.Type.PAYMENT);

        Payment renewedPayment = new Payment();
        renewedPayment.setId(55L);

        PaymentResponseDto expected = new PaymentResponseDto(
                55L, Payment.Status.PENDING, Payment.Type.PAYMENT, 100L, "http://new.url", "sess_renewed", new BigDecimal("90.00")
        );

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(100L, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.EXPIRED))
                .thenReturn(Optional.of(expiredPayment));
        when(paymentRepository.findById(55L)).thenReturn(Optional.of(expiredPayment));
        when(paymentRepository.save(expiredPayment)).thenReturn(renewedPayment);
        when(paymentMapper.toDto(renewedPayment)).thenReturn(expected);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session stripeSession = mock(Session.class);
            when(stripeSession.getId()).thenReturn("sess_renewed");
            when(stripeSession.getUrl()).thenReturn("http://new.url");
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(stripeSession);

            PaymentResponseDto actual = paymentService.createPayment(requestDto, authentication);

            assertThat(actual).isEqualTo(expected);
        }

        verify(paymentRepository, times(1)).findById(55L);
        verify(paymentRepository, times(1)).save(expiredPayment);
    }

    @Test
    @DisplayName("Create payment - StripeException thrown - should throw PaymentException")
    void createPayment_stripeException_shouldThrowPaymentException() {
        Authentication authentication = mock(Authentication.class);
        PaymentRequestDto requestDto = new PaymentRequestDto(100L, Payment.Type.PAYMENT);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(100L, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.EXPIRED))
                .thenReturn(Optional.empty());
        when(rentalRepository.findById(100L)).thenReturn(Optional.of(rental));

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenThrow(mock(StripeException.class));

            assertThrows(PaymentException.class, () -> paymentService.createPayment(requestDto, authentication));
        }
    }

    @Test
    @DisplayName("Create payment - as manager for another user - should create payment successfully")
    void createPayment_asManagerForOtherUser_shouldCreatePayment() {
        Authentication authentication = mock(Authentication.class);
        PaymentRequestDto requestDto = new PaymentRequestDto(100L, Payment.Type.PAYMENT);

        Payment savedPayment = new Payment();
        savedPayment.setId(1L);

        PaymentResponseDto expected = new PaymentResponseDto(
                1L, Payment.Status.PENDING, Payment.Type.PAYMENT, 100L, "url", "sess", new BigDecimal("90.00")
        );

        when(authentication.getPrincipal()).thenReturn(managerUser);
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(100L, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(100L, Payment.Type.PAYMENT, Payment.Status.EXPIRED))
                .thenReturn(Optional.empty());
        when(rentalRepository.findById(100L)).thenReturn(Optional.of(rental));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(paymentMapper.toDto(savedPayment)).thenReturn(expected);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session stripeSession = mock(Session.class);
            when(stripeSession.getId()).thenReturn("sess");
            when(stripeSession.getUrl()).thenReturn("url");
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(stripeSession);

            PaymentResponseDto actual = paymentService.createPayment(requestDto, authentication);

            assertThat(actual).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("Process successful payment - StripeException thrown - should throw PaymentException")
    void processSuccessfulPayment_stripeException_shouldThrowPaymentException() {
        String sessionId = "sess_123";
        Payment payment = new Payment();

        when(paymentRepository.findBySessionId(sessionId)).thenReturn(Optional.of(payment));

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.retrieve(sessionId)).thenThrow(mock(StripeException.class));

            assertThrows(PaymentException.class, () -> paymentService.processSuccessfulPayment(sessionId));
        }
    }

    @Test
    @DisplayName("Renew payment - payment ID not found - should throw EntityNotFoundException")
    void renewPayment_notFound_shouldThrowEntityNotFoundException() {
        Authentication authentication = mock(Authentication.class);
        Long paymentId = 99L;

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> paymentService.renewPayment(paymentId, authentication));

        verify(paymentRepository, times(1)).findById(paymentId);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Renew payment - customer has no permission for payment - should throw PaymentException")
    void renewPayment_noPermission_shouldThrowPaymentException() {
        Authentication authentication = mock(Authentication.class);
        Long paymentId = 1L;

        User anotherUser = new User();
        anotherUser.setId(88L);
        anotherUser.setRoles(customerUser.getRoles());

        Rental otherRental = new Rental();
        otherRental.setUser(anotherUser);

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setRental(otherRental);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class, () -> paymentService.renewPayment(paymentId, authentication));
    }

    @Test
    @DisplayName("Renew payment - StripeException thrown - should throw PaymentException")
    void renewPayment_stripeException_shouldThrowPaymentException() {
        Authentication authentication = mock(Authentication.class);
        Long paymentId = 1L;

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setRental(rental);
        payment.setStatus(Payment.Status.EXPIRED);
        payment.setAmountToPay(new BigDecimal("90.00"));
        payment.setType(Payment.Type.PAYMENT);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenThrow(mock(StripeException.class));

            assertThrows(PaymentException.class, () -> paymentService.renewPayment(paymentId, authentication));
        }
    }

    @Test
    @DisplayName("Check and update status - StripeException thrown - should handle exception gracefully")
    void checkAndUpdateStatus_stripeException_shouldCatchAndHandleGracefully() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setSessionId("sess_error");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.retrieve("sess_error")).thenThrow(mock(StripeException.class));

            paymentService.checkAndUpdateStatus(payment);
        }

        verify(paymentRepository, never()).save(any());
        verify(paymentRepository, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("Create payment - rental not found - should throw EntityNotFoundException")
    void createPayment_rentalNotFound_shouldThrowEntityNotFoundException() {
        Authentication authentication = mock(Authentication.class);
        Long rentalId = 999L;
        PaymentRequestDto requestDto = new PaymentRequestDto(rentalId, Payment.Type.PAYMENT);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(rentalId, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(false);
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(rentalId, Payment.Type.PAYMENT, Payment.Status.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(rentalId, Payment.Type.PAYMENT, Payment.Status.EXPIRED))
                .thenReturn(Optional.empty());
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> paymentService.createPayment(requestDto, authentication));

        verify(paymentRepository, times(1)).existsByRentalIdAndTypeAndStatus(rentalId, Payment.Type.PAYMENT, Payment.Status.PAID);
        verify(paymentRepository, times(1)).findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(rentalId, Payment.Type.PAYMENT, Payment.Status.PENDING);
        verify(paymentRepository, times(1)).findFirstByRentalIdAndTypeAndStatusOrderByCreatedAtDesc(rentalId, Payment.Type.PAYMENT, Payment.Status.EXPIRED);
        verify(rentalRepository, times(1)).findById(rentalId);
        verifyNoMoreInteractions(paymentRepository, rentalRepository);
        verifyNoInteractions(paymentMapper, notificationService);
    }

    @Test
    @DisplayName("init - should set Stripe API key")
    void init_shouldSetStripeApiKey() {
        String mockKey = "sk_test_mock_key";
        ReflectionTestUtils.setField(paymentService, "key", mockKey);

        paymentService.init();

        assertThat(Stripe.apiKey).isEqualTo(mockKey);
    }
}