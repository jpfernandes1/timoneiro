package com.jompastech.backend.unit.service;

import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private Booking booking;

    @Mock
    private Boat boat;

    @Mock
    private User user;

    private ListAppender<ILoggingEvent> listAppender;
    private ch.qos.logback.classic.Logger logger;

    @BeforeEach
    void setUp() {
        // Configurate log catch
        logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(NotificationServiceImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    void notifyOwner_shouldLogCorrectMessage() {
        // Arrange
        when(booking.getId()).thenReturn(123L);
        when(booking.getBoat()).thenReturn(boat);
        when(boat.getName()).thenReturn("Yacht Luxury");
        when(booking.getUser()).thenReturn(user);
        when(user.getEmail()).thenReturn("user@example.com");

        // Act
        notificationService.notifyOwner(booking);

        // Assert
        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs.get(0).getFormattedMessage())
                .contains("Notification to Owner: New booking #123 for boat 'Yacht Luxury' from user user@example.com.");
    }

    @Test
    void notifyRenter_shouldLogCorrectMessage() {
        // Arrange
        when(booking.getId()).thenReturn(456L);
        when(booking.getBoat()).thenReturn(boat);
        when(boat.getName()).thenReturn("Speed Boat");

        // Act
        notificationService.notifyRenter(booking);

        // Assert
        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs.get(0).getFormattedMessage())
                .contains("Notification to Renter: Your booking #456 for boat 'Speed Boat' is confirmed.");
    }

    @Test
    void notifyCancellation_shouldLogCorrectMessage() {
        // Arrange
        when(booking.getId()).thenReturn(789L);
        String reason = "Bad weather conditions";

        // Act
        notificationService.notifyCancellation(booking, reason);

        // Assert
        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs.get(0).getFormattedMessage())
                .contains("Cancellation Notification: Booking #789 was cancelled. Reason: Bad weather conditions");
    }

    @Test
    void notifyCancellation_withEmptyReason_shouldLogCorrectly() {
        // Arrange
        when(booking.getId()).thenReturn(999L);
        String reason = "";

        // Act
        notificationService.notifyCancellation(booking, reason);

        // Assert
        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs.get(0).getFormattedMessage())
                .contains("Cancellation Notification: Booking #999 was cancelled. Reason: ");
    }
}