package com.github.b1gsal.carsharingservice.service.rental;

import com.github.b1gsal.carsharingservice.dto.rental.RentalRequestDto;
import com.github.b1gsal.carsharingservice.dto.rental.RentalResponseDto;
import com.github.b1gsal.carsharingservice.exception.EntityNotFoundException;
import com.github.b1gsal.carsharingservice.exception.RentalException;
import com.github.b1gsal.carsharingservice.mapper.RentalMapper;
import com.github.b1gsal.carsharingservice.model.Car;
import com.github.b1gsal.carsharingservice.model.Payment;
import com.github.b1gsal.carsharingservice.model.Rental;
import com.github.b1gsal.carsharingservice.model.RoleName;
import com.github.b1gsal.carsharingservice.model.User;
import com.github.b1gsal.carsharingservice.repository.car.CarRepository;
import com.github.b1gsal.carsharingservice.repository.payment.PaymentRepository;
import com.github.b1gsal.carsharingservice.repository.rental.RentalRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class RentalServiceImpl implements RentalService {
    private final CarRepository carRepository;
    private final RentalMapper rentalMapper;
    private final RentalRepository rentalRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    @Override
    public RentalResponseDto rentCar(RentalRequestDto requestDto, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (paymentRepository.existsByRentalUserIdAndStatus(user.getId(), Payment.Status.PENDING)) {
            throw new RentalException("You have unpaid pending payments."
                    + " Please complete them before renting again.");
        }
        Car car = carRepository.findById(requestDto.carId()).orElseThrow(
                () -> new EntityNotFoundException("Invalid car's id " + requestDto.carId()));

        if (rentalRepository.existsByUserIdAndActualReturnDateIsNull(user.getId())) {
            throw new RentalException("You already have an active rental");
        }
        if (car.getInventory() < 1) {
            throw new RentalException("There is no more current type car " + car.getId());
        }

        car.setInventory(car.getInventory() - 1);
        carRepository.save(car);

        Rental model = rentalMapper.toModel(requestDto);
        model.setCar(car);
        model.setUser(user);
        model.setRentalDate(LocalDate.now());

        Rental savedRental = rentalRepository.save(model);
        return rentalMapper.toDto(savedRental);
    }

    @Transactional
    @Override
    public RentalResponseDto returnCar(Authentication authentication, Long id) {
        User user = (User) authentication.getPrincipal();

        Rental rental = rentalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find rental with id " + id));

        boolean isManager = user.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals(RoleName.ROLE_MANAGER.name()));

        if (!isManager && !rental.getUser().getId().equals(user.getId())) {
            throw new RentalException("You don't have permission to this rental");
        }

        if (rental.getActualReturnDate() != null) {
            throw new RentalException("This rental has already returned");
        }

        boolean isPaid = paymentRepository.existsByRentalIdAndTypeAndStatus(
                rental.getId(), Payment.Type.PAYMENT, Payment.Status.PAID);
        if (!isPaid) {
            throw new RentalException("Cannot return rental: initial payment is not completed");
        }

        Car car = rental.getCar();
        car.setInventory(car.getInventory() + 1);
        carRepository.save(car);

        rental.setActualReturnDate(LocalDate.now());
        return rentalMapper.toDto(rentalRepository.save(rental));
    }

    @Transactional(readOnly = true)
    @Override
    public RentalResponseDto getRental(Authentication authentication, Long id) {
        User user = (User) authentication.getPrincipal();

        boolean isManager = user.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority().equals(RoleName.ROLE_MANAGER.name()));

        Rental rental;

        if (isManager) {
            rental = rentalRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "There is no rental with current id " + id));
        } else {
            rental = rentalRepository.findByIdAndUserId(id, user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Can't find rental"));
        }

        return rentalMapper.toDto(rental);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<RentalResponseDto> getRentals(
            Long userId,
            Boolean isActive,
            Pageable pageable,
            Authentication authentication) {
        User curentUser = (User) authentication.getPrincipal();
        boolean isManager = curentUser.getAuthorities().stream()
                .anyMatch(role -> role.getAuthority()
                        .equals(RoleName.ROLE_MANAGER.name()));

        Long actualUserId = isManager ? userId : curentUser.getId();

        Page<Rental> rentalPage;
        if (actualUserId != null) {
            if (Boolean.TRUE.equals(isActive)) {
                rentalPage = rentalRepository
                        .findByUserIdAndActualReturnDateIsNull(actualUserId, pageable);
            } else if (Boolean.FALSE.equals(isActive)) {
                rentalPage = rentalRepository
                        .findByUserIdAndActualReturnDateIsNotNull(actualUserId, pageable);
            } else {
                rentalPage = rentalRepository
                        .findByUserId(actualUserId, pageable);
            }
        } else {
            if (Boolean.TRUE.equals(isActive)) {
                rentalPage = rentalRepository
                        .findByActualReturnDateIsNull(pageable);
            } else if (Boolean.FALSE.equals(isActive)) {
                rentalPage = rentalRepository
                        .findByActualReturnDateIsNotNull(pageable);
            } else {
                rentalPage = rentalRepository
                        .findAll(pageable);
            }
        }
        return rentalPage.map(rentalMapper::toDto);
    }
}
