package com.github.b1gsal.carsharingservice.service.rental;

import com.github.b1gsal.carsharingservice.dto.rental.RentalRequestDto;
import com.github.b1gsal.carsharingservice.dto.rental.RentalResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface RentalService {
    RentalResponseDto rentCar(RentalRequestDto requestDto, Authentication authentication);

    RentalResponseDto returnCar(Authentication authentication, Long id);

    RentalResponseDto getRental(Authentication authentication, Long id);

    Page<RentalResponseDto> getRentals(Long userId, Boolean isActive,
                                       Pageable pageable, Authentication authentication);
}
