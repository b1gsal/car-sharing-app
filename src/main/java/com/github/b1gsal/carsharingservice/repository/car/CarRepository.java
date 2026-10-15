package com.github.b1gsal.carsharingservice.repository.car;

import com.github.b1gsal.carsharingservice.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
}
