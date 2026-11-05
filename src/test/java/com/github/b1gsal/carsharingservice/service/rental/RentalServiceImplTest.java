package com.github.b1gsal.carsharingservice.service.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.b1gsal.carsharingservice.dto.rental.RentalRequestDto;
import com.github.b1gsal.carsharingservice.dto.rental.RentalResponseDto;
import com.github.b1gsal.carsharingservice.exception.EntityNotFoundException;
import com.github.b1gsal.carsharingservice.exception.RentalException;
import com.github.b1gsal.carsharingservice.mapper.RentalMapper;
import com.github.b1gsal.carsharingservice.model.Car;
import com.github.b1gsal.carsharingservice.model.Payment;
import com.github.b1gsal.carsharingservice.model.Rental;
import com.github.b1gsal.carsharingservice.model.Role;
import com.github.b1gsal.carsharingservice.model.RoleName;
import com.github.b1gsal.carsharingservice.model.User;
import com.github.b1gsal.carsharingservice.repository.car.CarRepository;
import com.github.b1gsal.carsharingservice.repository.payment.PaymentRepository;
import com.github.b1gsal.carsharingservice.repository.rental.RentalRepository;
import com.github.b1gsal.carsharingservice.service.notification.NotificationService;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class RentalServiceImplTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private RentalMapper rentalMapper;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RentalServiceImpl rentalService;

    private User customerUser;
    private User managerUser;
    private Car car;

    @BeforeEach
    void setUp() {
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

        car = new Car();
        car.setId(5L);
        car.setBrand("Skoda");
        car.setModel("Octavia");
        car.setInventory(3);
    }

    @Test
    @DisplayName("Rent car - valid request - should create rental and decrease inventory")
    void rentCar_validRequest_shouldCreateRental() {
        Authentication authentication = mock(Authentication.class);
        RentalRequestDto requestDto = new RentalRequestDto(LocalDate.now().plusDays(5), 5L);

        Rental rentalModel = new Rental();
        Rental savedRental = new Rental();
        savedRental.setId(100L);
        savedRental.setCar(car);
        savedRental.setUser(customerUser);

        RentalResponseDto expected = new RentalResponseDto(
                100L,
                LocalDate.now(),
                requestDto.returnDate(),
                null,
                5L,
                10L
        );

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalUserIdAndStatus(10L, Payment.Status.PENDING)).thenReturn(false);
        when(carRepository.findById(5L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByUserIdAndActualReturnDateIsNull(10L)).thenReturn(false);
        when(rentalMapper.toModel(requestDto)).thenReturn(rentalModel);
        when(carRepository.save(car)).thenReturn(car);
        when(rentalRepository.save(rentalModel)).thenReturn(savedRental);
        when(rentalMapper.toDto(savedRental)).thenReturn(expected);

        RentalResponseDto actual = rentalService.rentCar(requestDto, authentication);

        assertThat(actual).isEqualTo(expected);
        assertThat(car.getInventory()).isEqualTo(2);

        verify(paymentRepository, times(1)).existsByRentalUserIdAndStatus(10L, Payment.Status.PENDING);
        verify(carRepository, times(1)).findById(5L);
        verify(rentalRepository, times(1)).existsByUserIdAndActualReturnDateIsNull(10L);
        verify(carRepository, times(1)).save(car);
        verify(rentalRepository, times(1)).save(rentalModel);
        verify(notificationService, times(1)).notifyRentalCreated(savedRental);
        verify(rentalMapper, times(1)).toModel(requestDto);
        verify(rentalMapper, times(1)).toDto(savedRental);
        verifyNoMoreInteractions(carRepository, rentalRepository, paymentRepository, notificationService, rentalMapper);
    }

    @Test
    @DisplayName("Rent car - has pending payment - should throw RentalException")
    void rentCar_hasPendingPayment_shouldThrowRentalException() {
        Authentication authentication = mock(Authentication.class);
        RentalRequestDto requestDto = new RentalRequestDto(LocalDate.now().plusDays(5), 5L);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalUserIdAndStatus(10L, Payment.Status.PENDING)).thenReturn(true);

        assertThrows(RentalException.class, () -> rentalService.rentCar(requestDto, authentication));

        verify(paymentRepository, times(1)).existsByRentalUserIdAndStatus(10L, Payment.Status.PENDING);
        verifyNoMoreInteractions(paymentRepository);
        verifyNoInteractions(carRepository, rentalRepository, notificationService, rentalMapper);
    }

    @Test
    @DisplayName("Rent car - car not found - should throw EntityNotFoundException")
    void rentCar_carNotFound_shouldThrowEntityNotFoundException() {
        Authentication authentication = mock(Authentication.class);
        RentalRequestDto requestDto = new RentalRequestDto(LocalDate.now().plusDays(5), 99L);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalUserIdAndStatus(10L, Payment.Status.PENDING)).thenReturn(false);
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> rentalService.rentCar(requestDto, authentication));

        verify(paymentRepository, times(1)).existsByRentalUserIdAndStatus(10L, Payment.Status.PENDING);
        verify(carRepository, times(1)).findById(99L);
        verifyNoMoreInteractions(paymentRepository, carRepository);
        verifyNoInteractions(rentalRepository, notificationService, rentalMapper);
    }

    @Test
    @DisplayName("Rent car - user has active rental - should throw RentalException")
    void rentCar_hasActiveRental_shouldThrowRentalException() {
        Authentication authentication = mock(Authentication.class);
        RentalRequestDto requestDto = new RentalRequestDto(LocalDate.now().plusDays(5), 5L);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalUserIdAndStatus(10L, Payment.Status.PENDING)).thenReturn(false);
        when(carRepository.findById(5L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByUserIdAndActualReturnDateIsNull(10L)).thenReturn(true);

        assertThrows(RentalException.class, () -> rentalService.rentCar(requestDto, authentication));

        verify(paymentRepository, times(1)).existsByRentalUserIdAndStatus(10L, Payment.Status.PENDING);
        verify(carRepository, times(1)).findById(5L);
        verify(rentalRepository, times(1)).existsByUserIdAndActualReturnDateIsNull(10L);
        verifyNoMoreInteractions(paymentRepository, carRepository, rentalRepository);
        verifyNoInteractions(notificationService, rentalMapper);
    }

    @Test
    @DisplayName("Rent car - car inventory is 0 - should throw RentalException")
    void rentCar_noInventory_shouldThrowRentalException() {
        Authentication authentication = mock(Authentication.class);
        RentalRequestDto requestDto = new RentalRequestDto(LocalDate.now().plusDays(5), 5L);

        car.setInventory(0);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(paymentRepository.existsByRentalUserIdAndStatus(10L, Payment.Status.PENDING)).thenReturn(false);
        when(carRepository.findById(5L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByUserIdAndActualReturnDateIsNull(10L)).thenReturn(false);

        assertThrows(RentalException.class, () -> rentalService.rentCar(requestDto, authentication));

        verify(paymentRepository, times(1)).existsByRentalUserIdAndStatus(10L, Payment.Status.PENDING);
        verify(carRepository, times(1)).findById(5L);
        verify(rentalRepository, times(1)).existsByUserIdAndActualReturnDateIsNull(10L);
        verifyNoMoreInteractions(paymentRepository, carRepository, rentalRepository);
        verifyNoInteractions(notificationService, rentalMapper);
    }

    @Test
    @DisplayName("Return car - valid customer request - should return rental and increase inventory")
    void returnCar_validCustomerRequest_shouldReturnRental() {
        Authentication authentication = mock(Authentication.class);
        Long rentalId = 100L;

        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setUser(customerUser);
        rental.setCar(car);

        RentalResponseDto expected = new RentalResponseDto(
                rentalId,
                LocalDate.now().minusDays(2),
                LocalDate.now(),
                LocalDate.now(),
                5L,
                10L
        );

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(rentalId, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(true);
        when(carRepository.save(car)).thenReturn(car);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(rentalMapper.toDto(rental)).thenReturn(expected);

        RentalResponseDto actual = rentalService.returnCar(authentication, rentalId);

        assertThat(actual).isEqualTo(expected);
        assertThat(car.getInventory()).isEqualTo(4);
        assertThat(rental.getActualReturnDate()).isEqualTo(LocalDate.now());

        verify(rentalRepository, times(1)).findById(rentalId);
        verify(paymentRepository, times(1)).existsByRentalIdAndTypeAndStatus(rentalId, Payment.Type.PAYMENT, Payment.Status.PAID);
        verify(carRepository, times(1)).save(car);
        verify(rentalRepository, times(1)).save(rental);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, paymentRepository, carRepository, rentalMapper);
    }

    @Test
    @DisplayName("Return car - customer trying to return other user rental - should throw RentalException")
    void returnCar_noPermission_shouldThrowRentalException() {
        Authentication authentication = mock(Authentication.class);
        Long rentalId = 100L;

        User anotherUser = new User();
        anotherUser.setId(99L);

        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setUser(anotherUser);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

        assertThrows(RentalException.class, () -> rentalService.returnCar(authentication, rentalId));

        verify(rentalRepository, times(1)).findById(rentalId);
        verifyNoMoreInteractions(rentalRepository);
        verifyNoInteractions(paymentRepository, carRepository, rentalMapper);
    }

    @Test
    @DisplayName("Return car - already returned - should throw RentalException")
    void returnCar_alreadyReturned_shouldThrowRentalException() {
        Authentication authentication = mock(Authentication.class);
        Long rentalId = 100L;

        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setUser(customerUser);
        rental.setActualReturnDate(LocalDate.now().minusDays(1));

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

        assertThrows(RentalException.class, () -> rentalService.returnCar(authentication, rentalId));

        verify(rentalRepository, times(1)).findById(rentalId);
        verifyNoMoreInteractions(rentalRepository);
        verifyNoInteractions(paymentRepository, carRepository, rentalMapper);
    }

    @Test
    @DisplayName("Return car - initial payment not completed - should throw RentalException")
    void returnCar_paymentNotCompleted_shouldThrowRentalException() {
        Authentication authentication = mock(Authentication.class);
        Long rentalId = 100L;

        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setUser(customerUser);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(rentalId, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(false);

        assertThrows(RentalException.class, () -> rentalService.returnCar(authentication, rentalId));

        verify(rentalRepository, times(1)).findById(rentalId);
        verify(paymentRepository, times(1)).existsByRentalIdAndTypeAndStatus(rentalId, Payment.Type.PAYMENT, Payment.Status.PAID);
        verifyNoMoreInteractions(rentalRepository, paymentRepository);
        verifyNoInteractions(carRepository, rentalMapper);
    }

    @Test
    @DisplayName("Get rental by ID - as manager - should return rental")
    void getRental_asManager_shouldReturnRental() {
        Authentication authentication = mock(Authentication.class);
        Long rentalId = 100L;

        Rental rental = new Rental();
        rental.setId(rentalId);

        RentalResponseDto expected = new RentalResponseDto(rentalId, LocalDate.now(), LocalDate.now().plusDays(3), null, 5L, 10L);

        when(authentication.getPrincipal()).thenReturn(managerUser);
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        when(rentalMapper.toDto(rental)).thenReturn(expected);

        RentalResponseDto actual = rentalService.getRental(authentication, rentalId);

        assertThat(actual).isEqualTo(expected);

        verify(rentalRepository, times(1)).findById(rentalId);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, rentalMapper);
    }

    @Test
    @DisplayName("Get rental by ID - as customer - should return rental")
    void getRental_asCustomer_shouldReturnRental() {
        Authentication authentication = mock(Authentication.class);
        Long rentalId = 100L;

        Rental rental = new Rental();
        rental.setId(rentalId);

        RentalResponseDto expected = new RentalResponseDto(rentalId, LocalDate.now(), LocalDate.now().plusDays(3), null, 5L, 10L);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(rentalRepository.findByIdAndUserId(rentalId, 10L)).thenReturn(Optional.of(rental));
        when(rentalMapper.toDto(rental)).thenReturn(expected);

        RentalResponseDto actual = rentalService.getRental(authentication, rentalId);

        assertThat(actual).isEqualTo(expected);

        verify(rentalRepository, times(1)).findByIdAndUserId(rentalId, 10L);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, rentalMapper);
    }

    @Test
    @DisplayName("Get rentals - as customer - should return customer page of rentals")
    void getRentals_asCustomer_shouldReturnCustomerRentals() {
        Authentication authentication = mock(Authentication.class);
        Pageable pageable = PageRequest.of(0, 10);

        Rental rental = new Rental();
        rental.setId(100L);

        Page<Rental> rentalPage = new PageImpl<>(List.of(rental));
        RentalResponseDto dto = new RentalResponseDto(100L, LocalDate.now(), LocalDate.now().plusDays(3), null, 5L, 10L);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(rentalRepository.findByUserId(10L, pageable)).thenReturn(rentalPage);
        when(rentalMapper.toDto(rental)).thenReturn(dto);

        Page<RentalResponseDto> actual = rentalService.getRentals(null, null, pageable, authentication);

        assertThat(actual).isNotNull();
        assertThat(actual.getTotalElements()).isEqualTo(1);
        assertThat(actual.getContent().get(0)).isEqualTo(dto);

        verify(rentalRepository, times(1)).findByUserId(10L, pageable);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, rentalMapper);
    }

    @Test
    @DisplayName("Return car - rental not found - should throw EntityNotFoundException")
    void returnCar_rentalNotFound_shouldThrowEntityNotFoundException() {
        Authentication authentication = mock(Authentication.class);
        Long rentalId = 99L;

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> rentalService.returnCar(authentication, rentalId));

        verify(rentalRepository, times(1)).findById(rentalId);
        verifyNoMoreInteractions(rentalRepository);
        verifyNoInteractions(paymentRepository, carRepository, rentalMapper);
    }

    @Test
    @DisplayName("Return car - as manager for another user - should return rental")
    void returnCar_asManagerForOtherUser_shouldReturnRental() {
        Authentication authentication = mock(Authentication.class);
        Long rentalId = 100L;

        User otherUser = new User();
        otherUser.setId(99L);

        Rental rental = new Rental();
        rental.setId(rentalId);
        rental.setUser(otherUser);
        rental.setCar(car);

        RentalResponseDto expected = new RentalResponseDto(
                rentalId,
                LocalDate.now().minusDays(2),
                LocalDate.now(),
                LocalDate.now(),
                5L,
                99L
        );

        when(authentication.getPrincipal()).thenReturn(managerUser);
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        when(paymentRepository.existsByRentalIdAndTypeAndStatus(rentalId, Payment.Type.PAYMENT, Payment.Status.PAID))
                .thenReturn(true);
        when(carRepository.save(car)).thenReturn(car);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(rentalMapper.toDto(rental)).thenReturn(expected);

        RentalResponseDto actual = rentalService.returnCar(authentication, rentalId);

        assertThat(actual).isEqualTo(expected);
        verify(rentalRepository, times(1)).findById(rentalId);
        verify(paymentRepository, times(1)).existsByRentalIdAndTypeAndStatus(rentalId, Payment.Type.PAYMENT, Payment.Status.PAID);
        verify(carRepository, times(1)).save(car);
        verify(rentalRepository, times(1)).save(rental);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, paymentRepository, carRepository, rentalMapper);
    }

    @Test
    @DisplayName("Get rental by ID - as customer - rental not found - should throw EntityNotFoundException")
    void getRental_asCustomerNotFound_shouldThrowEntityNotFoundException() {
        Authentication authentication = mock(Authentication.class);
        Long rentalId = 99L;

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(rentalRepository.findByIdAndUserId(rentalId, 10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> rentalService.getRental(authentication, rentalId));

        verify(rentalRepository, times(1)).findByIdAndUserId(rentalId, 10L);
        verifyNoMoreInteractions(rentalRepository);
        verifyNoInteractions(rentalMapper);
    }

    @Test
    @DisplayName("Get rental by ID - as manager - rental not found - should throw EntityNotFoundException")
    void getRental_asManagerNotFound_shouldThrowEntityNotFoundException() {
        Authentication authentication = mock(Authentication.class);
        Long rentalId = 99L;

        when(authentication.getPrincipal()).thenReturn(managerUser);
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> rentalService.getRental(authentication, rentalId));

        verify(rentalRepository, times(1)).findById(rentalId);
        verifyNoMoreInteractions(rentalRepository);
        verifyNoInteractions(rentalMapper);
    }

    @Test
    @DisplayName("Get rentals - as customer with isActive true - should return active rentals")
    void getRentals_asCustomerActiveTrue_shouldReturnActiveRentals() {
        Authentication authentication = mock(Authentication.class);
        Pageable pageable = PageRequest.of(0, 10);
        Rental rental = new Rental();
        Page<Rental> rentalPage = new PageImpl<>(List.of(rental));
        RentalResponseDto dto = new RentalResponseDto(100L, LocalDate.now(), LocalDate.now().plusDays(3), null, 5L, 10L);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(rentalRepository.findByUserIdAndActualReturnDateIsNull(10L, pageable)).thenReturn(rentalPage);
        when(rentalMapper.toDto(rental)).thenReturn(dto);

        Page<RentalResponseDto> actual = rentalService.getRentals(null, true, pageable, authentication);

        assertThat(actual.getContent()).containsExactly(dto);
        verify(rentalRepository, times(1)).findByUserIdAndActualReturnDateIsNull(10L, pageable);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, rentalMapper);
    }

    @Test
    @DisplayName("Get rentals - as customer with isActive false - should return inactive rentals")
    void getRentals_asCustomerActiveFalse_shouldReturnInactiveRentals() {
        Authentication authentication = mock(Authentication.class);
        Pageable pageable = PageRequest.of(0, 10);
        Rental rental = new Rental();
        Page<Rental> rentalPage = new PageImpl<>(List.of(rental));
        RentalResponseDto dto = new RentalResponseDto(100L, LocalDate.now(), LocalDate.now().plusDays(3), LocalDate.now(), 5L, 10L);

        when(authentication.getPrincipal()).thenReturn(customerUser);
        when(rentalRepository.findByUserIdAndActualReturnDateIsNotNull(10L, pageable)).thenReturn(rentalPage);
        when(rentalMapper.toDto(rental)).thenReturn(dto);

        Page<RentalResponseDto> actual = rentalService.getRentals(null, false, pageable, authentication);

        assertThat(actual.getContent()).containsExactly(dto);
        verify(rentalRepository, times(1)).findByUserIdAndActualReturnDateIsNotNull(10L, pageable);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, rentalMapper);
    }

    @Test
    @DisplayName("Get rentals - as manager with specific userId and isActive true")
    void getRentals_asManagerWithUserIdAndActiveTrue_shouldReturnUserActiveRentals() {
        Authentication authentication = mock(Authentication.class);
        Pageable pageable = PageRequest.of(0, 10);
        Long targetUserId = 22L;
        Rental rental = new Rental();
        Page<Rental> rentalPage = new PageImpl<>(List.of(rental));
        RentalResponseDto dto = new RentalResponseDto(100L, LocalDate.now(), LocalDate.now().plusDays(3), null, 5L, targetUserId);

        when(authentication.getPrincipal()).thenReturn(managerUser);
        when(rentalRepository.findByUserIdAndActualReturnDateIsNull(targetUserId, pageable)).thenReturn(rentalPage);
        when(rentalMapper.toDto(rental)).thenReturn(dto);

        Page<RentalResponseDto> actual = rentalService.getRentals(targetUserId, true, pageable, authentication);

        assertThat(actual.getContent()).containsExactly(dto);
        verify(rentalRepository, times(1)).findByUserIdAndActualReturnDateIsNull(targetUserId, pageable);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, rentalMapper);
    }

    @Test
    @DisplayName("Get rentals - as manager with specific userId and isActive false")
    void getRentals_asManagerWithUserIdAndActiveFalse_shouldReturnUserInactiveRentals() {
        Authentication authentication = mock(Authentication.class);
        Pageable pageable = PageRequest.of(0, 10);
        Long targetUserId = 22L;
        Rental rental = new Rental();
        Page<Rental> rentalPage = new PageImpl<>(List.of(rental));
        RentalResponseDto dto = new RentalResponseDto(100L, LocalDate.now(), LocalDate.now().plusDays(3), LocalDate.now(), 5L, targetUserId);

        when(authentication.getPrincipal()).thenReturn(managerUser);
        when(rentalRepository.findByUserIdAndActualReturnDateIsNotNull(targetUserId, pageable)).thenReturn(rentalPage);
        when(rentalMapper.toDto(rental)).thenReturn(dto);

        Page<RentalResponseDto> actual = rentalService.getRentals(targetUserId, false, pageable, authentication);

        assertThat(actual.getContent()).containsExactly(dto);
        verify(rentalRepository, times(1)).findByUserIdAndActualReturnDateIsNotNull(targetUserId, pageable);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, rentalMapper);
    }

    @Test
    @DisplayName("Get rentals - as manager with userId null and isActive true - should return all active rentals")
    void getRentals_asManagerNoUserIdActiveTrue_shouldReturnAllActiveRentals() {
        Authentication authentication = mock(Authentication.class);
        Pageable pageable = PageRequest.of(0, 10);
        Rental rental = new Rental();
        Page<Rental> rentalPage = new PageImpl<>(List.of(rental));
        RentalResponseDto dto = new RentalResponseDto(100L, LocalDate.now(), LocalDate.now().plusDays(3), null, 5L, 10L);

        when(authentication.getPrincipal()).thenReturn(managerUser);
        when(rentalRepository.findByActualReturnDateIsNull(pageable)).thenReturn(rentalPage);
        when(rentalMapper.toDto(rental)).thenReturn(dto);

        Page<RentalResponseDto> actual = rentalService.getRentals(null, true, pageable, authentication);

        assertThat(actual.getContent()).containsExactly(dto);
        verify(rentalRepository, times(1)).findByActualReturnDateIsNull(pageable);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, rentalMapper);
    }

    @Test
    @DisplayName("Get rentals - as manager with userId null and isActive false - should return all inactive rentals")
    void getRentals_asManagerNoUserIdActiveFalse_shouldReturnAllInactiveRentals() {
        Authentication authentication = mock(Authentication.class);
        Pageable pageable = PageRequest.of(0, 10);
        Rental rental = new Rental();
        Page<Rental> rentalPage = new PageImpl<>(List.of(rental));
        RentalResponseDto dto = new RentalResponseDto(100L, LocalDate.now(), LocalDate.now().plusDays(3), LocalDate.now(), 5L, 10L);

        when(authentication.getPrincipal()).thenReturn(managerUser);
        when(rentalRepository.findByActualReturnDateIsNotNull(pageable)).thenReturn(rentalPage);
        when(rentalMapper.toDto(rental)).thenReturn(dto);

        Page<RentalResponseDto> actual = rentalService.getRentals(null, false, pageable, authentication);

        assertThat(actual.getContent()).containsExactly(dto);
        verify(rentalRepository, times(1)).findByActualReturnDateIsNotNull(pageable);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, rentalMapper);
    }

    @Test
    @DisplayName("Get rentals - as manager with userId null and isActive null - should return all rentals")
    void getRentals_asManagerNoUserIdActiveNull_shouldReturnAllRentals() {
        Authentication authentication = mock(Authentication.class);
        Pageable pageable = PageRequest.of(0, 10);
        Rental rental = new Rental();
        Page<Rental> rentalPage = new PageImpl<>(List.of(rental));
        RentalResponseDto dto = new RentalResponseDto(100L, LocalDate.now(), LocalDate.now().plusDays(3), null, 5L, 10L);

        when(authentication.getPrincipal()).thenReturn(managerUser);
        when(rentalRepository.findAll(pageable)).thenReturn(rentalPage);
        when(rentalMapper.toDto(rental)).thenReturn(dto);

        Page<RentalResponseDto> actual = rentalService.getRentals(null, null, pageable, authentication);

        assertThat(actual.getContent()).containsExactly(dto);
        verify(rentalRepository, times(1)).findAll(pageable);
        verify(rentalMapper, times(1)).toDto(rental);
        verifyNoMoreInteractions(rentalRepository, rentalMapper);
    }
}