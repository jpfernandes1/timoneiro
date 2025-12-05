package com.jompastech.backend.service;

import com.jompastech.backend.model.entity.Booking;

public interface NotificationService {
    void notifyOwner(Booking booking);
    void notifyRenter(Booking booking);
    void notifyCancellation(Booking booking, String reason);
}