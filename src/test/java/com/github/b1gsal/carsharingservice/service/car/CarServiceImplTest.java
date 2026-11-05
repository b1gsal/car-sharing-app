package com.github.b1gsal.carsharingservice.service.car;

import com.github.b1gsal.carsharingservice.dto.car.CarRequestDto;
import com.github.b1gsal.carsharingservice.dto.car.CarResponseDto;
import com.github.b1gsal.carsharingservice.dto.car.CarUpdateDto;
import com.github.b1gsal.carsharingservice.exception.EntityNotFoundException;
import com.github.b1gsal.carsharingservice.mapper.CarMapper;
import com.github.b1gsal.carsharingservice.model.Car;
import com.github.b1gsal.carsharingservice.model.Type;
import com.github.b1gsal.carsharingservice.repository.car.CarRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarServiceImplTest {
    private static final int PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 10;

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarMapper carMapper;

    @InjectMocks
    private CarServiceImpl carService;


    @Test
    @DisplayName("Add car - valid request - should return CarResponseDto")
    void addCar_validData_ShouldReturnCarDto() {
        CarRequestDto carRequestDto = new CarRequestDto(
                "Octavia",
                "Skoda",
                Type.SEDAN,
                new BigDecimal("30.00"),
                3);

        Car car = new Car();
        car.setModel("Octavia");
        car.setBrand("Skoda");
        car.setDailyFee(new BigDecimal("30.00"));
        car.setType(Type.SEDAN);
        car.setInventory(3);


        Car savedCar = new Car();
        savedCar.setId(1L);
        savedCar.setModel("Octavia");
        savedCar.setBrand("Skoda");
        savedCar.setDailyFee(new BigDecimal("30.00"));
        savedCar.setType(Type.SEDAN);
        savedCar.setInventory(3);
        savedCar.setDeleted(false);

        CarResponseDto carResponseDto = new CarResponseDto(
                1L,
                "Octavia",
                "Skoda",
                Type.SEDAN,
                new BigDecimal("30.00"),
                3);

        when(carMapper.toModel(carRequestDto)).thenReturn(car);
        when(carRepository.save(car)).thenReturn(savedCar);
        when(carMapper.toDto(savedCar)).thenReturn(carResponseDto);

        CarResponseDto actual = carService.addCar(carRequestDto);
        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(carResponseDto);

        verify(carMapper, times(1)).toModel(carRequestDto);
        verify(carRepository, times(1)).save(car);
        verify(carMapper, times(1)).toDto(savedCar);

        verifyNoMoreInteractions(carRepository, carMapper);
    }

    @Test
    @DisplayName("Get car info by id - valid request - should return CarResponseDto")
    void getCarInfo_validData_shouldReturnCarDto() {
        Long carId = 1L;

        Car carFromDb = new Car();
        carFromDb.setId(1L);
        carFromDb.setModel("Octavia");
        carFromDb.setBrand("Skoda");
        carFromDb.setDailyFee(new BigDecimal("30.00"));
        carFromDb.setType(Type.SEDAN);
        carFromDb.setInventory(3);
        carFromDb.setDeleted(false);

        CarResponseDto carResponseDto = new CarResponseDto(
                1L,
                "Octavia",
                "Skoda",
                Type.SEDAN,
                new BigDecimal("30.00"),
                3);

        when(carRepository.findById(carId)).thenReturn(Optional.of(carFromDb));
        when(carMapper.toDto(carFromDb)).thenReturn(carResponseDto);

        CarResponseDto actual = carService.getCarInfo(carId);

        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(carResponseDto);

        verify(carRepository, times(1)).findById(carId);
        verify(carMapper, times(1)).toDto(carFromDb);

        verifyNoMoreInteractions(carRepository, carMapper);
    }

    @Test
    @DisplayName("Get car info by id - invalid request - should throw EntityNotFoundException")
    void getCarInfo_invalidData_shouldThrowEntityNotFoundException() {
        Long carId = 10L;
        String expected = "Can't find car with id " + carId;

        when(carRepository.findById(carId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> carService.getCarInfo(carId));

        String actual = exception.getMessage();

        assertThat(actual).isEqualTo(expected);

        verify(carRepository, times(1)).findById(carId);

        verifyNoMoreInteractions(carRepository);
        verifyNoInteractions(carMapper);
    }

    @Test
    @DisplayName("Delete car - valid id - should delete car by id")
    void deleteById_validId_shouldDeleteCar() {
        Long id = 1L;

        carService.deleteById(id);

        verify(carRepository, times(1)).deleteById(id);

        verifyNoMoreInteractions(carRepository);
        verifyNoInteractions(carMapper);
    }

    @Test
    @DisplayName("Update car - valid request - should update car")
    void update_validRequest_shouldUpdateCar() {
        Long id = 1L;

        CarUpdateDto carUpdateDto = new CarUpdateDto(
                "Superb",
                "Skoda",
                Type.SEDAN,
                new BigDecimal("30.00"),
                4);

        Car carFromDb = new Car();
        carFromDb.setId(1L);
        carFromDb.setModel("Octavia");
        carFromDb.setBrand("Skoda");
        carFromDb.setDailyFee(new BigDecimal("30.00"));
        carFromDb.setType(Type.SEDAN);
        carFromDb.setInventory(3);
        carFromDb.setDeleted(false);

        CarResponseDto carResponseDto = new CarResponseDto(
                1L,
                "Superb",
                "Skoda",
                Type.SEDAN,
                new BigDecimal("30.00"),
                4);

        doAnswer(invocation -> {
            CarUpdateDto sourceDto = invocation.getArgument(1);
            Car targetModel = invocation.getArgument(0);

            targetModel.setModel(sourceDto.model());
            targetModel.setInventory(sourceDto.inventory());

            return null;
        }).when(carMapper).updateCarFromDto(carFromDb, carUpdateDto);

        when(carRepository.save(carFromDb)).thenReturn(carFromDb);

        when(carRepository.findById(id)).thenReturn(Optional.of(carFromDb));

        when(carMapper.toDto(carFromDb)).thenReturn(carResponseDto);

        CarResponseDto actual = carService.update(carUpdateDto, id);

        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(carResponseDto);

        verify(carRepository, times(1)).findById(id);
        verify(carRepository, times(1)).save(carFromDb);
        verify(carMapper, times(1)).updateCarFromDto(carFromDb, carUpdateDto);
        verify(carMapper, times(1)).toDto(carFromDb);

        verifyNoMoreInteractions(carRepository, carMapper);
    }

    @Test
    @DisplayName("Update car - invalid request - should throw EntityNotFoundException")
    void update_invalidData_ShouldThrowEntityNotFoundException() {
        Long id = 10L;
        CarUpdateDto carUpdateDto = null;

        String expected = "Can't find car with id " + id;

        when(carRepository.findById(id)).thenReturn(Optional.empty());


        EntityNotFoundException entityNotFoundException = assertThrows(EntityNotFoundException.class,
                () -> carService.update(carUpdateDto, id));

        String actual = entityNotFoundException.getMessage();
        assertThat(actual).isEqualTo(expected);

        verify(carRepository, times(1)).findById(id);
        verifyNoMoreInteractions(carRepository);
        verifyNoInteractions(carMapper);
    }

    @Test
    @DisplayName("Get all cars - valid request - should return Page of cars")
    void getCars_validRequest_shouldReturnPageOfCars() {
        Pageable pageable = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);

        Car car = new Car();
        car.setId(1L);

        CarResponseDto carResponseDto = new CarResponseDto(
                1L,
                "Octavia",
                "Skoda",
                Type.SEDAN,
                new BigDecimal("30.00"),
                3
        );

        Page<Car> carPage = new PageImpl<>(List.of(car), pageable, 1);

        when(carRepository.findAll(pageable)).thenReturn(carPage);
        when(carMapper.toDto(car)).thenReturn(carResponseDto);

        Page<CarResponseDto> actual = carService.getCars(pageable);

        assertThat(actual).isNotNull();
        assertThat(actual.getTotalElements()).isEqualTo(1);
        assertThat(actual.getContent().get(0)).isEqualTo(carResponseDto);

        verify(carRepository, times(1)).findAll(pageable);
        verify(carMapper, times(1)).toDto(car);
        verifyNoMoreInteractions(carRepository, carMapper);
    }

}