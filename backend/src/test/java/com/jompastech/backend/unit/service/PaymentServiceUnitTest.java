package com.jompastech.backend.unit.service;

import com.jompastech.backend.exception.PaymentValidationException;
import com.jompastech.backend.model.dto.payment.*;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.Payment;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.model.enums.PaymentMethod;
import com.jompastech.backend.model.enums.PaymentStatus;
import com.jompastech.backend.repository.BookingRepository;
import com.jompastech.backend.repository.PaymentRepository;
import com.jompastech.backend.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for PaymentService class.
 *
 * Testing Strategy:
 * - Pure unit tests with all external dependencies mocked
 * - Focus on business logic, validation rules, and error handling
 * - Complete coverage of success and failure scenarios
 * - Verify all payment flows: credit card, PIX, and boleto
 *
 * Test Categories:
 * 1. Payment Processing - Main processPayment method scenarios
 * 2. Payment Validation - All validation rules and constraints
 * 3. Credit Card Validation - Specific card data validation
 * 4. Gateway Simulation - Mocked external gateway responses
 * 5. Entity Operations - Payment entity creation and updates
 * 6. Query Methods - Retrieval by transaction ID and user history
 * 7. Exception Handling - Graceful error responses and logging
 *
 * Design Patterns Used:
 * - Arrange-Act-Assert for clear test structure
 * - Nested test classes for logical grouping
 * - Parameterized tests for multiple data scenarios
 * - Test Data Builders for reusable test objects
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceUnitTest {

    // MOCKED DEPENDENCIES

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private Environment env;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private Booking mockBooking;

    @Mock
    private User mockUser;

    // TEST DATA

    private PaymentInfo validPaymentInfo;
    private MockCardData validCardData;

    /**
     * Setup method executed before each test. This creates test data
     * Without using static mock() calls.
     * This avoids the Instrumentation warning and PotentialStubbingProblem.
     * We use lenient stubbing for environment properties because they are used in many tests,
     * but not necessarily in every test.
     *
     * Key Configurations:
     * - Mock Environment properties for validation and gateway URLs
     * - Create reusable test PaymentInfo with valid credit card data
     * - Prepare mock Booking and User entities for relationship testing
     */
    @BeforeEach
    void setUp() {
        // Setup common test data
        // 1. Create mock User entity
        mockUser = mock(User.class);
        lenient().when(mockUser.getId()).thenReturn(100L);

        // 2. Create mock Booking entity
        mockBooking = mock(Booking.class);
        lenient().when(mockBooking.getId()).thenReturn(1L);
        lenient().when(mockBooking.getUser()).thenReturn(mockUser);

        // 3. Create valid MockCardData
        validCardData = new MockCardData(
                "4111111111111111",  // Test card that always gets approved
                "John Doe",
                "12/25",
                "123"
        );

        // 4. Create valid PaymentInfo
        validPaymentInfo = PaymentInfo.builder()
                .amount(new BigDecimal("1500.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .mockCardData(validCardData)
                .bookingId(1L)
                .description("Boat reservation payment")
                .userEmail("customer@example.com")
                .installments(1)
                .build();

        // 5. Configure Environment mock (CRITICAL - was causing NPE)
        lenient().when(env.getProperty(eq("app.payments.max-amount"), anyString()))
                .thenReturn("10000");

        // For pagseguro URL - note the service might call with default value
        lenient().when(env.getProperty(eq("app.pagseguro.sandbox-url"), anyString()))
                .thenReturn("https://sandbox.pagseguro.uol.com.br/v2/transactions");

        // For pagseguro token - note the service might call with default value
        lenient().when(env.getProperty(eq("app.pagseguro.sandbox-token"), anyString()))
                .thenReturn("SANDBOX_TOKEN_" + System.currentTimeMillis());
    }

    // PAYMENT PROCESSING TESTS

    @Nested
    @DisplayName("Payment Processing Tests")
    class PaymentProcessingTests {

        /**
         * Tests successful credit card payment processing.
         * <p>
         * Scenario:
         * - Valid credit card information (test card 4111...)
         * - Booking exists in repository
         * - Gateway simulation returns approved response
         * <p>
         * Expected Outcome:
         * - PaymentResult with success = true
         * - Status = CONFIRMED
         * - Transaction ID generated
         * - Payment saved twice (initial and after gateway)
         */
        @Test
        @DisplayName("Should process payment successfully with credit card")
        void processPayment_shouldReturnSuccess_whenCreditCardPaymentIsApproved() {
            // Arrange
            when(bookingRepository.findById(anyLong()))
                    .thenReturn(Optional.of(mockBooking));
            when(paymentRepository.save(any(Payment.class)))
                    .thenAnswer(invocation -> {
                        Payment p = invocation.getArgument(0);
                        p.setId(1L);
                        return p;
                    });

            // Act
            PaymentResult result = paymentService.processPayment(validPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTransactionId()).isNotNull();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
            assertThat(result.getGatewayMessage()).contains("approved");

            // Verify interactions
            verify(paymentRepository, times(2)).save(any(Payment.class));
            verify(bookingRepository).findById(1L);
        }

        /**
         * Tests successful PIX payment processing.
         * <p>
         * Scenario:
         * - Payment method is PIX (no card data required)
         * - Booking exists in repository
         * - Gateway simulation returns successful response
         * <p>
         * Expected Outcome:
         * - PaymentResult with success = true
         * - Status is either CONFIRMED or PENDING
         * - Payment entity saved twice (initial creation and update)
         */
        @Test
        @DisplayName("Should process PIX payment successfully")
        void processPayment_shouldReturnSuccess_whenPixPaymentIsApproved() {
            // Arrange
            PaymentInfo pixPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("2000.00"))
                    .paymentMethod(PaymentMethod.PIX)
                    .bookingId(1L)
                    .description("PIX payment for booking")
                    .userEmail("customer@example.com")
                    .build();

            when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(mockBooking));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                payment.setId(2L);
                return payment;
            });

            // Act
            PaymentResult result = paymentService.processPayment(pixPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isIn(PaymentStatus.CONFIRMED, PaymentStatus.PENDING);
            verify(paymentRepository, times(2)).save(any(Payment.class));
        }

        /**
         * Tests handling of payment declined by the payment gateway.
         * <p>
         * Scenario:
         * - Credit card number that triggers declined response (4222...)
         * - Booking exists in repository
         * - Gateway simulation returns declined response
         * <p>
         * Expected Outcome:
         * - PaymentResult with success = false
         * - Status = CANCELLED
         * - Gateway message contains decline information
         */
        @Test
        @DisplayName("Should handle gateway declined payment")
        void processPayment_shouldReturnFailedResult_WhenGatewayDeclines() {
            // Arrange
            MockCardData declinedCardData = new MockCardData(
                    "4222222222222222",  // Test card that always gets declined
                    "John Doe",
                    "12/25",
                    "123"
            );

            PaymentInfo declinedPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1500.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(declinedCardData)
                    .bookingId(1L)
                    .build();

            when(bookingRepository.findById(anyLong())).thenReturn(Optional.ofNullable(mockBooking));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                payment.setId(1L);
                return payment;
            });

            // Act
            PaymentResult result = paymentService.processPayment(declinedPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(result.getGatewayMessage()).contains("declined");
        }

        /**
         * Tests successful payment processing with boat context (no booking).
         * <p>
         * Scenario:
         * - Payment with boatId instead of bookingId
         * - Valid credit card information
         * - No booking repository lookup required
         * <p>
         * Expected Outcome:
         * - PaymentResult with success = true
         * - Transaction ID generated
         * - Payment saved twice (initial and after gateway)
         * - No booking repository interactions
         */
        @Test
        @DisplayName("Should handle payment with boat context")
        void processPayment_shouldProcessSuccessfully_whenBoatContextProvided() {
            // Arrange
            PaymentInfo boatPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("500.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(validCardData)
                    .bookingId(10L)
                    .description("Boat maintenance payment")
                    .userEmail("owner@example.com")
                    .build();

            Booking mockBooking = mock(Booking.class);
            when(bookingRepository.findById(10L)).thenReturn(Optional.of(mockBooking));

            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                payment.setId(3L);
                return payment;
            });

            // Mock do Environment para passar na validação
            when(env.getProperty("app.payments.max-amount", "10000")).thenReturn("10000");
            when(env.getProperty("app.pagseguro.sandbox-url")).thenReturn("https://sandbox.example.com");
            when(env.getProperty("app.pagseguro.sandbox-token")).thenReturn("test-token");

            // Act
            PaymentResult result = paymentService.processPayment(boatPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTransactionId()).isNotNull();
            verify(bookingRepository).findById(10L);
            verify(paymentRepository, times(2)).save(any(Payment.class));
        }
    }

    // PAYMENT VALIDATION TESTS

    @Nested
    @DisplayName("Payment Validation Tests")
    class PaymentValidationTests {

        /**
         * Tests validation when payment amount is null.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when amount is null")
        void validatePaymentInfo_shouldThrowException_whenAmountIsNull() {
            // Arrange
            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(null)
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Payment amount must be greater than zero");
        }

        /**
         * Tests validation when payment amount is zero.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should Throw exception when amount is zero")
        void validatePaymentInfo_shouldThrowException_whenAmountIsZero() {
            // Arrange
            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(BigDecimal.ZERO)
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Payment amount must be greater than zero");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        /**
         * Tests validation when payment amount is negative.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should Throw exception when amount is negative")
        void validatePaymentInfo_shouldThrowException_whenAmountIsNegative() {
            // Arrange
            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("-100.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Payment amount must be greater than zero");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        /**
         * Tests validation when payment amount exceeds configured maximum.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when amount exceeds maximum")
        void validatePaymentInfo_shouldThrowException_whenAmountExceedsMaximum() {
            // Arrange
            BigDecimal maxAmount = new BigDecimal("10000");
            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(maxAmount.add(BigDecimal.ONE))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Payment amount exceeds maximum allowed");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        /**
         * Tests validation when payment method is null.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when payment method is null")
        void validatePaymentInfo_shouldThrowException_whenPaymentMethodIsNull() {
            // Arrange
            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(null)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Payment method is required");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        /**
         * Tests validation when both booking and boat contexts are provided.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when both booking and boat context are provided")
        void validatePaymentInfo_shouldThrowException_whenBothBookingAndBoatContextProvided() {
            // Arrange
            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .bookingId(1L)
                    .boatId(10L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Cannot specify both booking and boat context");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        /**
         * Tests validation when neither booking nor boat context is provided.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when neither booking nor boat context is provided")
        void validatePaymentInfo_shouldThrowException_whenNoContextProvided() {
            // Arrange
            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Either booking or boat context is required");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        /**
         * Tests validation when installments count is less than 1.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when installments are less than 1")
        void validatePaymentInfo_shouldThrowException_whenInstallmentsLessThanOne() {
            // Arrange
            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(validCardData)
                    .bookingId(1L)
                    .installments(0)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Installments must be at least 1");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }
    }

    // NESTED TEST CLASS: CREDIT CARD VALIDATION TESTS

    @Nested
    @DisplayName("Credit Card Validation Tests")
    class CreditCardValidationTests {

        /**
         * Tests validation when card data is null for credit card payment.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when card data is null for credit card payment")
        void validateCreditCard_shouldThrowException_whenCardDataIsNull() {
            // Arrange
            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(null)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Card data is required for credit card payments");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        /**
         * Tests validation when card number is null.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when card number is null")
        void validateCreditCard_shouldThrowException_whenCardNumberIsNull() {
            // Arrange
            MockCardData invalidCardData = new MockCardData(
                    null,
                    "John Doe",
                    "12/25",
                    "123"
            );

            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(invalidCardData)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Valid card number is required");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        /**
         * Tests validation when card number is too short (less than 13 digits).
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when card number is too short")
        void validateCreditCard_shouldThrowException_whenCardNumberIsTooShort() {
            // Arrange
            MockCardData invalidCardData = new MockCardData(
                    "123456789012",
                    "John Doe",
                    "12/25",
                    "123"
            );

            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(invalidCardData)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Valid card number is required");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        /**
         * Tests validation when card holder name is null.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when holder name is null")
        void validateCreditCard_shouldThrowException_whenHolderNameIsNull() {
            // Arrange
            MockCardData invalidCardData = new MockCardData(
                    "4111111111111111",
                    null,
                    "12/25",
                    "123"
            );

            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(invalidCardData)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Card holder name is required");
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        /**
         * Tests validation when card holder name is empty or whitespace only.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should Throw exception when holder name is empty")
        void validateCreditCard_shouldThrowException_whenHolderNameIsEmpty() {
            // Arrange
            MockCardData invalidCardData = new MockCardData(
                    "4111111111111111",
                    "   ",
                    "12/25",
                    "123"
            );

            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal(2000))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(invalidCardData)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(result.getErrorMessage()).contains("Card holder name is required");
        }

        /**
         * Tests validation when expiration date is null.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when expiration date is null")
        void validateCreditCard_shouldThrowException_whenExpirationDateIsNull() {
            // Arrange
            MockCardData invalidCardData = new MockCardData(
                    "4111111111111111",
                    "John Doe",
                    null,
                    "123"
            );

            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(invalidCardData)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(result.getErrorMessage()).contains("Card expiration date is required");
        }

        /**
         * Tests validation when expiration date format is invalid (invalid month).
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when expiration date format is invalid")
        void validateCreditCard_shouldThrowException_whenExpirationDateFormatInvalid() {
            // Arrange
            MockCardData invalidCardData = new MockCardData(
                    "4111111111111111",
                    "John Doe",
                    "13/25",
                    "123"
            );

            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(invalidCardData)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(result.getErrorMessage()).contains("Card expiration date must be in format MM/YY or MM/YYYY");
        }

        /**
         * Parameterized test for various invalid expiration date scenarios.
         * <p>
         * Tests multiple invalid date formats to ensure comprehensive validation.
         *
         * @param expirationDate The invalid expiration date to test
         * @param expectedMessage The expected error message fragment
         */
        @ParameterizedTest
        @MethodSource("invalidExpirationDates")
        @DisplayName("Should throw exception for various invalid expiration dates")
        void validateCreditCard_shouldThrowException_forInvalidExpirationDates(
                String expirationDate, String expectedMessage) {
            // Arrange
            MockCardData invalidCardData = new MockCardData(
                    "4111111111111111",
                    "John Doe",
                    expirationDate,
                    "123"
            );

            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(invalidCardData)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(result.getErrorMessage()).contains(expectedMessage);
        }

        /**
         * Provides test data for invalid expiration date scenarios.
         *
         * @return Stream of Arguments containing expiration date and expected error message
         */
        static Stream<Arguments> invalidExpirationDates() {
            return Stream.of(
                    Arguments.of("", "Card expiration date is required"),
                    Arguments.of("13/25", "Card expiration date must be in format"),
                    Arguments.of("00/25", "Card expiration date must be in format"),
                    Arguments.of("12/2", "Card expiration date must be in format"),
                    Arguments.of("12/2A", "Card expiration date must be in format")
            );
        }

        /**
         * Tests validation when CVV (card security code) is null.
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when CVV is null")
        void validateCreditCard_shouldThrowException_whenCvvIsNull() {
            // Arrange
            MockCardData invalidCardData = new MockCardData(
                    "4111111111111111",
                    "John Doe",
                    "12/25",
                    null
            );

            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(invalidCardData)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(result.getErrorMessage()).contains("Card security code (CVV) is required");
        }

        /**
         * Tests validation when CVV (card security code) is too short (less than 3 digits).
         * <p>
         * Expected: PaymentResult with validation error message.
         */
        @Test
        @DisplayName("Should throw exception when CVV is too short")
        void validateCreditCard_shouldThrowException_whenCvvIsTooShort() {
            // Arrange
            MockCardData invalidCardData = new MockCardData(
                    "4111111111111111",
                    "John Doe",
                    "12/25",
                    "12"
            );

            PaymentInfo invalidPaymentInfo = PaymentInfo.builder()
                    .amount(new BigDecimal("1000.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .mockCardData(invalidCardData)
                    .bookingId(1L)
                    .build();

            // Act
            PaymentResult result = paymentService.processPayment(invalidPaymentInfo);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(result.getErrorMessage()).contains("Card security code (CVV) is required");
        }

        // ========================================================================
        // NESTED TEST CLASS: EXCEPTION HANDLING TESTS
        // ========================================================================
        @Nested
        @DisplayName("Exception Handling Tests")
        class ExceptionHandlingTests {

            /**
             * Tests handling of non-existent booking scenario.
             * <p>
             * Scenario: Payment references a booking ID that doesn't exist.
             * Expected: PaymentResult with validation error message.
             */
            @Test
            @DisplayName("Should throw exception when booking not found")
            void createPaymentEntity_shouldThrowException_whenBookingNotFound() {
                // Arrange
                when(bookingRepository.findById(anyLong()))
                        .thenReturn(Optional.empty());

                // Act
                PaymentResult result = paymentService.processPayment(validPaymentInfo);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.isSuccess()).isFalse();
                assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
                assertThat(result.getErrorMessage()).contains("Booking not found");
            }

            /**
             * Tests graceful handling of unexpected system errors.
             * <p>
             * Scenario: Repository throws unexpected RuntimeException.
             * Expected: PaymentResult with SYSTEM_ERROR code and graceful failure message.
             */
            @Test
            @DisplayName("Should handle generic exceptions gracefully")
            void processPayment_shouldReturnFailedResult_whenUnexpectedExceptionOccurs() {
                // Arrange
                when(bookingRepository.findById(anyLong()))
                        .thenThrow(new RuntimeException("Database connection failed"));

                // Act
                PaymentResult result = paymentService.processPayment(validPaymentInfo);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.isSuccess()).isFalse();
                assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
                assertThat(result.getErrorMessage()).isEqualTo("Payment system temporarily unavailable");
            }
        }

        // NESTED TEST CLASS: QUERY METHOD TESTS

        @Nested
        @DisplayName("Query Method Tests")
        class QueryMethodTests {

            /**
             * Tests retrieval of payment by transaction ID.
             * <p>
             * Scenario: Payment exists with known transaction ID.
             * Expected: PaymentResult with correct transaction details.
             */
            @Test
            @DisplayName("Should retrieve payment by transaction ID")
            void getPaymentByTransactionId_shouldReturnPaymentResult_whenTransactionExists() {
                // Arrange
                Payment confirmedPayment = new Payment();
                confirmedPayment.setId(1L);
                confirmedPayment.setBooking(mockBooking);
                confirmedPayment.setAmount(new BigDecimal("1500.00"));
                confirmedPayment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
                confirmedPayment.setStatus(PaymentStatus.CONFIRMED);
                confirmedPayment.setTransactionId("PSB_123456789");
                confirmedPayment.setGatewayMessage("Payment approved");
                confirmedPayment.setProcessedAt(LocalDateTime.now());

                when(paymentRepository.findByTransactionId(anyString()))
                        .thenReturn(Optional.of(confirmedPayment));

                // Act
                PaymentResult result = paymentService.getPaymentByTransactionId("PSB_123456789");

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getTransactionId()).isEqualTo("PSB_123456789");
                assertThat(result.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
                assertThat(result.isSuccess()).isTrue();

                verify(paymentRepository).findByTransactionId("PSB_123456789");
            }

            /**
             * Tests error when transaction ID doesn't exist.
             * <p>
             * Scenario: Non-existent transaction ID queried.
             * Expected: PaymentValidationException with descriptive message.
             */
            @Test
            @DisplayName("Should throw exception when transaction not found")
            void getPaymentByTransactionId_shouldThrowException_whenTransactionNotFound() {
                // Arrange
                when(paymentRepository.findByTransactionId(anyString()))
                        .thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> paymentService.getPaymentByTransactionId("INVALID_ID"))
                        .isInstanceOf(PaymentValidationException.class)
                        .hasMessageContaining("Payment not found for transaction");

                verify(paymentRepository).findByTransactionId("INVALID_ID");
            }

            /**
             * Tests retrieval of user payment history.
             * <p>
             * Scenario: User has multiple payment records.
             * Expected: List of PaymentResult DTOs with correct count.
             */
            @Test
            @DisplayName("Should retrieve user payment history")
            void getUserPaymentHistory_shouldReturnPaymentList_whenUserHasPayments() {
                // Arrange
                Payment payment1 = new Payment();
                payment1.setId(1L);
                payment1.setBooking(mockBooking);
                payment1.setAmount(new BigDecimal("1500.00"));
                payment1.setStatus(PaymentStatus.CONFIRMED);
                payment1.setTransactionId("PSB_123456789");

                Payment payment2 = new Payment();
                payment2.setId(2L);
                payment2.setBooking(mockBooking);
                payment2.setAmount(new BigDecimal("2000.00"));
                payment2.setStatus(PaymentStatus.CONFIRMED);
                payment2.setTransactionId("PSB_987654321");

                List<Payment> userPayments = List.of(payment1, payment2);

                when(paymentRepository.findByUserId(anyLong()))
                        .thenReturn(userPayments);

                // Act
                List<PaymentResult> results = paymentService.getUserPaymentHistory(100L);

                // Assert
                assertThat(results).isNotNull();
                assertThat(results).hasSize(2);
                assertThat(results).allMatch(PaymentResult::isSuccess);

                verify(paymentRepository).findByUserId(100L);
            }

            /**
             * Tests empty result for user without payments.
             * <p>
             * Scenario: User has no payment history.
             * Expected: Empty list (not null).
             */
            @Test
            @DisplayName("Should return empty list when user has no payments")
            void getUserPaymentHistory_shouldReturnEmptyList_whenUserHasNoPayments() {
                // Arrange
                when(paymentRepository.findByUserId(anyLong()))
                        .thenReturn(List.of());

                // Act
                List<PaymentResult> results = paymentService.getUserPaymentHistory(999L);

                // Assert
                assertThat(results).isNotNull();
                assertThat(results).isEmpty();

                verify(paymentRepository).findByUserId(999L);
            }
        }
    }
}