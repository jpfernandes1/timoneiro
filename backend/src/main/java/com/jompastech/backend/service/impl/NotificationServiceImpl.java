package com.jompastech.backend.service.impl;


import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void notifyOwner(Booking booking) {
        // TODO: Integrate with email service (e.g., SendGrid, AWS SES)
        log.info("Notification to Owner: New booking #{} for boat '{}' from user {}.",
                booking.getId(), booking.getBoat().getName(), booking.getUser().getEmail());
    }

    @Override
    public void notifyRenter(Booking booking) {
        // TODO: Integrate with email service
        log.info("Notification to Renter: Your booking #{} for boat '{}' is confirmed.",
                booking.getId(), booking.getBoat().getName());
    }

    @Override
    public void notifyCancellation(Booking booking, String reason) {
        log.info("Cancellation Notification: Booking #{} was cancelled. Reason: {}",
                booking.getId(), reason);
    }
}