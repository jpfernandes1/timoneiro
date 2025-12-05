package com.jompastech.backend.Unit.service;

import com.jompastech.backend.exception.BookingCreationException;
import com.jompastech.backend.exception.PaymentProcessingException;
import com.jompastech.backend.model.dto.booking.CreateBookingCommand;
import com.jompastech.backend.model.dto.payment.MockCardData;
import com.jompastech.backend.model.dto.payment.PaymentInfo;
import com.jompastech.backend.model.dto.payment.PaymentResult;
import com.jompastech.backend.model.entity.Boat;
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
    private CreateBookingCommand validCommand;
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

        validCommand = new CreateBookingCommand();
        validCommand.setUserId(1L);
        validCommand.setBoatId(1L);
        validCommand.setStartDate(startDate);
        validCommand.setEndDate(endDate);
        validCommand.setTotalPrice(new BigDecimal("500.00"));
        validCommand.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        MockCardData mockCardData = new MockCardData(
                "4111111111111111",
                "Test User",
                "12/30",
                "123"
                );
        validCommand.setMockCardData(mockCardData);
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
            Booking result = bookingApplicationService.createBooking(validCommand);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

            // Verify interactions
            verify(userRepository).findById(1L);
            verify(boatRepository).findById(1L);
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
            assertThat(capturedPaymentInfo.getDescription()).contains("Boat rental - 1");
            assertThat(capturedPaymentInfo.getMockCardData()).isNotNull();
        }

        @Test
        @DisplayName("Should process PIX payment without card data")
        void createBooking_WithPixPayment_ShouldProcessWithoutCardData() {
            // Arrange
            CreateBookingCommand pixCommand = new CreateBookingCommand();
            pixCommand.setUserId(1L);
            pixCommand.setBoatId(1L);
            pixCommand.setStartDate(startDate);
            pixCommand.setEndDate(endDate);
            pixCommand.setTotalPrice(new BigDecimal("300.00"));
            pixCommand.setPaymentMethod(PaymentMethod.PIX);
            // No mockCardData for PIX

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

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
            Booking result = bookingApplicationService.createBooking(pixCommand);

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
            assertThatThrownBy(() -> bookingApplicationService.createBooking(validCommand))
                    .isInstanceOf(BookingCreationException.class)
                    .hasMessageContaining("User not found with id: 1");

            verify(userRepository).findById(1L);
            verifyNoInteractions(boatRepository, bookingValidationService, paymentService, bookingRepository, notificationService);
        }

        @Test
        @DisplayName("Should throw BookingCreationException when boat not found")
        void createBooking_WithNonExistentBoat_ShouldThrowBookingCreationException() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> bookingApplicationService.createBooking(validCommand))
                    .isInstanceOf(BookingCreationException.class)
                    .hasMessageContaining("Boat not found with id: 1");

            verify(userRepository).findById(1L);
            verify(boatRepository).findById(1L);
            verifyNoInteractions(bookingValidationService, paymentService, bookingRepository, notificationService);
        }

        @Test
        @DisplayName("Should throw exception when validation fails")
        void createBooking_WithFailedValidation_ShouldThrowBookingValidationException() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

            doThrow(new com.jompastech.backend.exception.BookingValidationException("Validation failed"))
                    .when(bookingValidationService).validateBookingCreation(any(Booking.class));

            // Act & Assert
            assertThatThrownBy(() -> bookingApplicationService.createBooking(validCommand))
                    .isInstanceOf(com.jompastech.backend.exception.BookingValidationException.class)
                    .hasMessageContaining("Validation failed");

            verify(userRepository).findById(1L);
            verify(boatRepository).findById(1L);
            verify(bookingValidationService).validateBookingCreation(any(Booking.class));
            verifyNoInteractions(paymentService, bookingRepository, notificationService);
        }

        @Test
        @DisplayName("Should throw PaymentProcessingException when payment fails")
        void createBooking_WithFailedPayment_ShouldThrowPaymentProcessingException() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

            PaymentResult failedPayment = PaymentResult.builder()
                    .success(false)
                    .status(PaymentStatus.CANCELLED)
                    .errorMessage("Insufficient funds")
                    .processedAt(LocalDateTime.now())
                    .build();
            when(paymentService.processPayment(any(PaymentInfo.class))).thenReturn(failedPayment);

            // Act & Assert
            assertThatThrownBy(() -> bookingApplicationService.createBooking(validCommand))
                    .isInstanceOf(PaymentProcessingException.class)
                    .hasMessageContaining("Payment failed: Insufficient funds");

            verify(userRepository).findById(1L);
            verify(boatRepository).findById(1L);
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

            // Simulate unexpected exception in PaymentService
            when(paymentService.processPayment(any(PaymentInfo.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert
            assertThatThrownBy(() -> bookingApplicationService.createBooking(validCommand))
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

            PaymentResult successfulPayment = PaymentResult.builder()
                    .success(true)
                    .transactionId("TXN_12345")
                    .status(PaymentStatus.CONFIRMED)
                    .gatewayMessage("Payment approved")
                    .processedAt(LocalDateTime.now())
                    .build();
            when(paymentService.processPayment(any(PaymentInfo.class))).thenReturn(successfulPayment);

            // Use ArgumentCaptor to verify Booking status before saving
            Booking bookingToSave = new Booking(testUser, testBoat, startDate, endDate, new BigDecimal("500.00"));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
                Booking saved = invocation.getArgument(0);
                assertThat(saved.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
                return saved;
            });

            // Act
            bookingApplicationService.createBooking(validCommand);

            // Assert
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should build PaymentInfo correctly with all fields")
        void createBooking_ShouldBuildPaymentInfoCorrectly() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

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
            bookingApplicationService.createBooking(validCommand);

            // Assert
            verify(paymentService).processPayment(paymentInfoCaptor.capture());
            PaymentInfo paymentInfo = paymentInfoCaptor.getValue();

            assertThat(paymentInfo).isNotNull();
            assertThat(paymentInfo.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(paymentInfo.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
            assertThat(paymentInfo.getUserEmail()).isEqualTo("test@example.com");
            assertThat(paymentInfo.getDescription()).isEqualTo("Boat rental - 1");
            assertThat(paymentInfo.getMockCardData()).isNotNull();
            assertThat(paymentInfo.getMockCardData().getCardNumber()).isEqualTo("4111111111111111");

        }

        @Test
        @DisplayName("Should notify both owner and renter")
        void createBooking_ShouldNotifyBothParties() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

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
            Booking result = bookingApplicationService.createBooking(validCommand);

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
        CreateBookingCommand invalidDateCommand = new CreateBookingCommand();
        invalidDateCommand.setUserId(1L);
        invalidDateCommand.setBoatId(1L);
        invalidDateCommand.setStartDate(LocalDateTime.now().plusDays(2)); // Future start
        invalidDateCommand.setEndDate(LocalDateTime.now().plusDays(1));   // Past end (invalid)
        invalidDateCommand.setTotalPrice(new BigDecimal("200.00"));
        invalidDateCommand.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        MockCardData mockCardData = new MockCardData(
                "4111111111111111",
                "Test User",
                "12/30",
                "123"
        );
        invalidDateCommand.setMockCardData(mockCardData);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));

        // Act & Assert
        // Validation occurs inside Booking constructor (called by toBooking)
        // which throws IllegalArgumentException
        assertThatThrownBy(() -> bookingApplicationService.createBooking(invalidDateCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }
}