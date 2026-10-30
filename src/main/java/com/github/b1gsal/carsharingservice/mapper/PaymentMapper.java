package com.github.b1gsal.carsharingservice.mapper;

import com.github.b1gsal.carsharingservice.config.MapperConfig;
import com.github.b1gsal.carsharingservice.dto.payment.PaymentResponseDto;
import com.github.b1gsal.carsharingservice.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface PaymentMapper {

    @Mapping(target = "rentalId", source = "rental.id")
    PaymentResponseDto toDto(Payment payment);
}
