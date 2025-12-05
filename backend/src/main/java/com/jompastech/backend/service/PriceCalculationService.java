package com.jompastech.backend.service;

import com.jompastech.backend.model.entity.Boat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
public class PriceCalculationService {

    private static final int MINIMUM_BOOKING_HOURS = 4;

    public BigDecimal calculateBookingPrice(Boat boat, LocalDateTime startDate, LocalDateTime endDate) {
        if (boat == null || boat.getPricePerHour() == null) {
            throw new IllegalArgumentException("Boat and price per hour must not be null");
        }

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Invalid date range");
        }

        BigDecimal hourlyRate = boat.getPricePerHour();
        long hours = Duration.between(startDate, endDate).toHours();

        // Ensure minimum booking duration
        long effectiveHours = Math.max(hours, MINIMUM_BOOKING_HOURS);

        return hourlyRate.multiply(BigDecimal.valueOf(effectiveHours));
    }
}