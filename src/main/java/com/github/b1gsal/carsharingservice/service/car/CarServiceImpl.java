package com.github.b1gsal.carsharingservice.service.car;

import com.github.b1gsal.carsharingservice.dto.car.CarRequestDto;
import com.github.b1gsal.carsharingservice.dto.car.CarResponseDto;
import com.github.b1gsal.carsharingservice.dto.car.CarUpdateDto;
import com.github.b1gsal.carsharingservice.exception.EntityNotFoundException;
import com.github.b1gsal.carsharingservice.mapper.CarMapper;
import com.github.b1gsal.carsharingservice.model.Car;
import com.github.b1gsal.carsharingservice.repository.car.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CarServiceImpl implements CarService {
    private final CarRepository repository;
    private final CarMapper carMapper;

    @Override
    public Page<CarResponseDto> getCars(Pageable pageable) {
        return repository.findAll(pageable)
                .map(carMapper::toDto);
    }

    @Override
    public CarResponseDto addCar(CarRequestDto requestDto) {
        Car model = carMapper.toModel(requestDto);
        Car savedCar = repository.save(model);
        return carMapper.toDto(savedCar);
    }

    @Override
    public CarResponseDto getCarInfo(Long id) {
        Car car = repository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find car with id " + id));
        return carMapper.toDto(car);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public CarResponseDto update(CarUpdateDto requestDto, Long id) {
        Car car = repository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can't find car with id " + id));
        carMapper.updateCarFromDto(car, requestDto);
        return carMapper.toDto(repository.save(car));
    }
}
