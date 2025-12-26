package com.jompastech.backend.Unit.service;

import com.jompastech.backend.exception.BookingCreationException;
import com.jompastech.backend.exception.PaymentProcessingException;
import com.jompastech.backend.model.dto.booking.BookingRequestDTO;
import com.jompastech.backend.model.dto.payment.MockCardData;
import com.jompastech.backend.model.dto.payment.PaymentInfo;
import com.jompastech.backend.model.dto.payment.PaymentResult;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.model.enums.BookingStatus;
import com.jompastech.backend.model.enums.PaymentMethod;
import com.jompastech.backend.model.enums.PaymentStatus;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.repository.BookingRepository;
import com.jompastech.backend.repository.UserRepository;
import com.jompastech.backend.service.BookingApplicationService;
import com.jompastech.backend.service.BookingValidationService;
import com.jompastech.backend.service.NotificationService;
import com.jompastech.backend.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingApplicationService
 *
 * Focus: Test the booking creation flow including validations,
 * payment processing, and notifications.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Booking Application Service Tests")
class BookingApplicationServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BoatRepository boatRepository;

    @Mock
    private com.jompastech.backend.repository.BoatAvailabilityRepository boatAvailabilityRepository;

    @Mock
    private BookingValidationService bookingValidationService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingApplicationService bookingApplicationService;

    @Captor
    private ArgumentCaptor<PaymentInfo> paymentInfoCaptor;

    @Captor
    private ArgumentCaptor<Booking> bookingCaptor;

    private User testUser;
    private Boat testBoat;
    private BookingRequestDTO validRequest;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.now().plusDays(1);
        endDate = startDate.plusHours(4);

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        testBoat = new Boat();
        testBoat.setId(1L);
        testBoat.setName("Test Boat");
        testBoat.setOwner(new User());

        validRequest = new BookingRequestDTO();
        validRequest.setUserId(1L);
        validRequest.setBoatId(1L);
        validRequest.setStartDate(startDate);
        validRequest.setEndDate(endDate);
        validRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        MockCardData mockCardData = new MockCardData(
                "4111111111111111",
                "Test User",
                "12/30",
                "123"
        );
        validRequest.setMockCardData(mockCardData);
    }

    @Nested
    @DisplayName("Successful Booking Creation")
    class SuccessfulBookingCreation {

        @Test
        @DisplayName("Should create booking successfully with valid data")
        void createBooking_WithValidData_ShouldCreateBookingSuccessfully() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

            // Mock boat availability repository
            BoatAvailability availability =
                    new BoatAvailability();
            availability.setPricePerHour(new BigDecimal("125.00")); // 4 hours * 125 = 500
            when(boatAvailabilityRepository.findCoveringAvailabilityWindow(any(), any(), any()))
                    .thenReturn(java.util.Collections.singletonList(availability));

            PaymentResult successfulPayment = PaymentResult.builder()
                    .success(true)
                    .transactionId("TXN_12345")
                    .status(PaymentStatus.CONFIRMED)
                    .gatewayMessage("Payment approved")
                    .processedAt(LocalDateTime.now())
                    .build();
            when(paymentService.processPayment(any(PaymentInfo.class))).thenReturn(successfulPayment);

            Booking savedBooking = new Booking(testUser, testBoat, startDate, endDate, new BigDecimal("500.00"));
            savedBooking.confirm();
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            // Act
            Booking result = bookingApplicationService.createBooking(validRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

            // Verify interactions
            verify(userRepository).findById(1L);
            verify(boatRepository).findById(1L);
            verify(boatAvailabilityRepository).findCoveringAvailabilityWindow(any(), any(), any());
            verify(bookingValidationService).validateBookingCreation(any(Booking.class));
            verify(paymentService).processPayment(paymentInfoCaptor.capture());
            verify(bookingRepository).save(any(Booking.class));
            verify(notificationService).notifyOwner(any(Booking.class));
            verify(notificationService).notifyRenter(any(Booking.class));

            // Verify PaymentInfo built correctly
            PaymentInfo capturedPaymentInfo = paymentInfoCaptor.getValue();
            assertThat(capturedPaymentInfo.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(capturedPaymentInfo.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
            assertThat(capturedPaymentInfo.getUserEmail()).isEqualTo("test@example.com");
            assertThat(capturedPaymentInfo.getDescription()).contains("Boat rental");
            assertThat(capturedPaymentInfo.getMockCardData()).isNotNull();
        }

        @Test
        @DisplayName("Should process PIX payment without card data")
        void createBooking_WithPixPayment_ShouldProcessWithoutCardData() {
            // Arrange
            BookingRequestDTO pixRequest = new BookingRequestDTO();
            pixRequest.setUserId(1L);
            pixRequest.setBoatId(1L);
            pixRequest.setStartDate(startDate);
            pixRequest.setEndDate(endDate);
            pixRequest.setPaymentMethod(PaymentMethod.PIX);
            // No mockCardData for PIX

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

            // Mock boat availability repository
            BoatAvailability availability =
                    new BoatAvailability();
            availability.setPricePerHour(new BigDecimal("75.00")); // 4 hours * 75 = 300
            when(boatAvailabilityRepository.findCoveringAvailabilityWindow(any(), any(), any()))
                    .thenReturn(java.util.Collections.singletonList(availability));

            PaymentResult successfulPayment = PaymentResult.builder()
                    .success(true)
                    .transactionId("PIX_12345")
                    .status(PaymentStatus.CONFIRMED)
                    .gatewayMessage("PIX payment approved")
                    .processedAt(LocalDateTime.now())
                    .build();
            when(paymentService.processPayment(any(PaymentInfo.class))).thenReturn(successfulPayment);

            Booking savedBooking = new Booking(testUser, testBoat, startDate, endDate, new BigDecimal("300.00"));
            savedBooking.confirm();
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            // Act
            Booking result = bookingApplicationService.createBooking(pixRequest);

            // Assert
            verify(paymentService).processPayment(paymentInfoCaptor.capture());
            PaymentInfo paymentInfo = paymentInfoCaptor.getValue();

            assertThat(paymentInfo.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
            assertThat(paymentInfo.getMockCardData()).isNull(); // PIX doesn't need card data
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Error Scenarios")
    class ErrorScenarios {

        @Test
        @DisplayName("Should throw BookingCreationException when user not found")
        void createBooking_WithNonExistentUser_ShouldThrowBookingCreationException() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> bookingApplicationService.createBooking(validRequest))
                    .isInstanceOf(BookingCreationException.class)
                    .hasMessageContaining("User not found with id: 1");

            verify(userRepository).findById(1L);
            verifyNoInteractions(boatRepository, boatAvailabilityRepository, bookingValidationService, paymentService, bookingRepository, notificationService);
        }

        @Test
        @DisplayName("Should throw BookingCreationException when boat not found")
        void createBooking_WithNonExistentBoat_ShouldThrowBookingCreationException() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> bookingApplicationService.createBooking(validRequest))
                    .isInstanceOf(BookingCreationException.class)
                    .hasMessageContaining("Boat not found with id: 1");

            verify(userRepository).findById(1L);
            verify(boatRepository).findById(1L);
            verifyNoInteractions(boatAvailabilityRepository, bookingValidationService, paymentService, bookingRepository, notificationService);
        }

        @Test
        @DisplayName("Should throw exception when validation fails")
        void createBooking_WithFailedValidation_ShouldThrowBookingValidationException() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

            // Mock boat availability repository
            BoatAvailability availability =
                    new BoatAvailability();
            availability.setPricePerHour(new BigDecimal("125.00"));
            when(boatAvailabilityRepository.findCoveringAvailabilityWindow(any(), any(), any()))
                    .thenReturn(java.util.Collections.singletonList(availability));

            doThrow(new com.jompastech.backend.exception.BookingValidationException("Validation failed"))
                    .when(bookingValidationService).validateBookingCreation(any(Booking.class));

            // Act & Assert
            assertThatThrownBy(() -> bookingApplicationService.createBooking(validRequest))
                    .isInstanceOf(com.jompastech.backend.exception.BookingValidationException.class)
                    .hasMessageContaining("Validation failed");

            verify(userRepository).findById(1L);
            verify(boatRepository).findById(1L);
            verify(boatAvailabilityRepository).findCoveringAvailabilityWindow(any(), any(), any());
            verify(bookingValidationService).validateBookingCreation(any(Booking.class));
            verifyNoInteractions(paymentService, bookingRepository, notificationService);
        }

        @Test
        @DisplayName("Should throw PaymentProcessingException when payment fails")
        void createBooking_WithFailedPayment_ShouldThrowPaymentProcessingException() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

            // Mock boat availability repository
            BoatAvailability availability =
                    new BoatAvailability();
            availability.setPricePerHour(new BigDecimal("125.00"));
            when(boatAvailabilityRepository.findCoveringAvailabilityWindow(any(), any(), any()))
                    .thenReturn(java.util.Collections.singletonList(availability));

            PaymentResult failedPayment = PaymentResult.builder()
                    .success(false)
                    .status(PaymentStatus.CANCELLED)
                    .errorMessage("Insufficient funds")
                    .processedAt(LocalDateTime.now())
                    .build();
            when(paymentService.processPayment(any(PaymentInfo.class))).thenReturn(failedPayment);

            // Act & Assert
            assertThatThrownBy(() -> bookingApplicationService.createBooking(validRequest))
                    .isInstanceOf(PaymentProcessingException.class)
                    .hasMessageContaining("Payment failed: Insufficient funds");

            verify(userRepository).findById(1L);
            verify(boatRepository).findById(1L);
            verify(boatAvailabilityRepository).findCoveringAvailabilityWindow(any(), any(), any());
            verify(bookingValidationService).validateBookingCreation(any(Booking.class));
            verify(paymentService).processPayment(any(PaymentInfo.class));
            verifyNoInteractions(bookingRepository, notificationService);
        }

        @Test
        @DisplayName("Should propagate unexpected exceptions")
        void createBooking_WithUnexpectedException_ShouldPropagateException() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

            // Mock boat availability repository
            BoatAvailability availability =
                    new BoatAvailability();
            availability.setPricePerHour(new BigDecimal("125.00"));
            when(boatAvailabilityRepository.findCoveringAvailabilityWindow(any(), any(), any()))
                    .thenReturn(java.util.Collections.singletonList(availability));

            // Simulate unexpected exception in PaymentService
            when(paymentService.processPayment(any(PaymentInfo.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert
            assertThatThrownBy(() -> bookingApplicationService.createBooking(validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database connection failed");
        }
    }

    @Nested
    @DisplayName("Business Logic Verification")
    class BusinessLogicVerification {

        @Test
        @DisplayName("Should call confirm before saving booking")
        void createBooking_ShouldCallConfirmBeforeSaving() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

            // Mock boat availability repository
            BoatAvailability availability =
                    new BoatAvailability();
            availability.setPricePerHour(new BigDecimal("125.00"));
            when(boatAvailabilityRepository.findCoveringAvailabilityWindow(any(), any(), any()))
                    .thenReturn(java.util.Collections.singletonList(availability));

            PaymentResult successfulPayment = PaymentResult.builder()
                    .success(true)
                    .transactionId("TXN_12345")
                    .status(PaymentStatus.CONFIRMED)
                    .gatewayMessage("Payment approved")
                    .processedAt(LocalDateTime.now())
                    .build();
            when(paymentService.processPayment(any(PaymentInfo.class))).thenReturn(successfulPayment);

            // Use ArgumentCaptor to verify Booking status before saving
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
                Booking saved = invocation.getArgument(0);
                assertThat(saved.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
                return saved;
            });

            // Act
            bookingApplicationService.createBooking(validRequest);

            // Assert
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should build PaymentInfo correctly with all fields")
        void createBooking_ShouldBuildPaymentInfoCorrectly() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

            // Mock boat availability repository
            BoatAvailability availability =
                    new BoatAvailability();
            availability.setPricePerHour(new BigDecimal("125.00"));
            when(boatAvailabilityRepository.findCoveringAvailabilityWindow(any(), any(), any()))
                    .thenReturn(java.util.Collections.singletonList(availability));

            PaymentResult successfulPayment = PaymentResult.builder()
                    .success(true)
                    .transactionId("TXN_12345")
                    .status(PaymentStatus.CONFIRMED)
                    .gatewayMessage("Payment approved")
                    .processedAt(LocalDateTime.now())
                    .build();
            when(paymentService.processPayment(any(PaymentInfo.class))).thenReturn(successfulPayment);

            Booking savedBooking = new Booking(testUser, testBoat, startDate, endDate, new BigDecimal("500.00"));
            savedBooking.confirm();
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            // Act
            bookingApplicationService.createBooking(validRequest);

            // Assert
            verify(paymentService).processPayment(paymentInfoCaptor.capture());
            PaymentInfo paymentInfo = paymentInfoCaptor.getValue();

            assertThat(paymentInfo).isNotNull();
            assertThat(paymentInfo.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(paymentInfo.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
            assertThat(paymentInfo.getUserEmail()).isEqualTo("test@example.com");
            assertThat(paymentInfo.getDescription()).contains("Boat rental");
            assertThat(paymentInfo.getMockCardData()).isNotNull();
            assertThat(paymentInfo.getMockCardData().getCardNumber()).isEqualTo("4111111111111111");
        }

        @Test
        @DisplayName("Should notify both owner and renter")
        void createBooking_ShouldNotifyBothParties() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

            // Mock boat availability repository
            BoatAvailability availability =
                    new BoatAvailability();
            availability.setPricePerHour(new BigDecimal("125.00"));
            when(boatAvailabilityRepository.findCoveringAvailabilityWindow(any(), any(), any()))
                    .thenReturn(java.util.Collections.singletonList(availability));

            PaymentResult successfulPayment = PaymentResult.builder()
                    .success(true)
                    .transactionId("TXN_12345")
                    .status(PaymentStatus.CONFIRMED)
                    .gatewayMessage("Payment approved")
                    .processedAt(LocalDateTime.now())
                    .build();
            when(paymentService.processPayment(any(PaymentInfo.class))).thenReturn(successfulPayment);

            Booking savedBooking = new Booking(testUser, testBoat, startDate, endDate, new BigDecimal("500.00"));
            savedBooking.confirm();
            when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

            // Act
            Booking result = bookingApplicationService.createBooking(validRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(notificationService).notifyOwner(savedBooking);
            verify(notificationService).notifyRenter(savedBooking);

            // Verify order of calls (optional)
            verify(notificationService, times(1)).notifyOwner(savedBooking);
            verify(notificationService, times(1)).notifyRenter(savedBooking);
        }
    }

    @Test
    @DisplayName("Should fail with invalid dates in domain validation")
    void createBooking_WithInvalidDates_ShouldFailInDomainValidation() {
        // Arrange
        BookingRequestDTO invalidDateRequest = new BookingRequestDTO();
        invalidDateRequest.setUserId(1L);
        invalidDateRequest.setBoatId(1L);
        invalidDateRequest.setStartDate(LocalDateTime.now().plusDays(2)); // Future start
        invalidDateRequest.setEndDate(LocalDateTime.now().plusDays(1));   // Past end (invalid)
        invalidDateRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        MockCardData mockCardData = new MockCardData(
                "4111111111111111",
                "Test User",
                "12/30",
                "123"
        );
        invalidDateRequest.setMockCardData(mockCardData);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

        // Mock boat availability repository
        BoatAvailability availability =
                new BoatAvailability();
        availability.setPricePerHour(new BigDecimal("50.00"));
        when(boatAvailabilityRepository.findCoveringAvailabilityWindow(any(), any(), any()))
                .thenReturn(java.util.Collections.singletonList(availability));

        // Act & Assert
        // Validation occurs inside Booking constructor (called by service)
        // which throws IllegalArgumentException
        assertThatThrownBy(() -> bookingApplicationService.createBooking(invalidDateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }
}