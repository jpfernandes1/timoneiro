package com.jompastech.backend.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jompastech.backend.model.dto.payment.MockCardData;
import com.jompastech.backend.model.dto.payment.PaymentRequestDTO;
import com.jompastech.backend.model.entity.*;
import com.jompastech.backend.model.enums.PaymentMethod;
import com.jompastech.backend.model.enums.PaymentStatus;
import com.jompastech.backend.repository.*;
import com.jompastech.backend.security.filter.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static com.jompastech.backend.integration.controller.util.CpfGenerator.generateValidCpf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Slf4j
class PaymentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoatRepository boatRepository;

    @Autowired
    private BoatAvailabilityRepository boatAvailabilityRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // Test data
    private Long userId;
    private String jwtToken;
    private User testUser;
    private Boat testBoat;
    private BoatAvailability testAvailability;
    private Booking testBooking;
    private String bookingStartStr;
    private String bookingEndStr;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // -----------------------------------------------------------------
    //  Helper methods
    // -----------------------------------------------------------------

    private MockCardData approvedCard() {
        return new MockCardData(
                "4111111111111111",  // approved
                "John Doe",
                "12/2030",
                "123"
        );
    }

    private MockCardData declinedCard() {
        return new MockCardData(
                "4222222222222222",  // declined
                "John Doe",
                "12/2030",
                "123"
        );
    }

    private MockCardData pendingCard() {
        return new MockCardData(
                "4333333333333333",  // pending
                "John Doe",
                "12/2030",
                "123"
        );
    }

    private String cardToJson(MockCardData card) throws Exception {
        return objectMapper.writeValueAsString(card);
    }

    /**
     * Calculates HMAC-SHA256 in hexadecimal format.
     * Must match the implementation in PaymentService.
     */
    private String hmacSha256Base64(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------
    //  Setup and cleanup
    // -----------------------------------------------------------------

    @BeforeEach
    void setup() throws Exception {
        // 1. Create a unique user and obtain JWT token
        String userEmail = "payment_test_" + System.nanoTime() + "@boat.com";
        String validCpf = generateValidCpf();

        String userJson = String.format("""
            {
              "name": "Payment Test User",
              "email": "%s",
              "password": "asd@12345",
              "cpf": "%s",
              "phone": "11999999999"
            }
            """, userEmail, validCpf);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());

        String loginJson = String.format("""
            {
              "email": "%s",
              "password": "asd@12345"
            }
            """, userEmail);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(loginResponse);
        this.jwtToken = root.get("token").asText();
        this.testUser = userRepository.findByEmail(userEmail).orElseThrow();
        this.userId = testUser.getId();

        // 2. Create a boat
        String boatJson = """
            {
              "name": "Boat for Payment Tests",
              "description": "Boat used in payment integration tests",
              "type": "LANCHA",
              "capacity": 8,
              "pricePerHour": 300,
              "cep": "12345-000",
              "number": "10",
              "street": "Test Street",
              "neighborhood": "Test Neighborhood",
              "city": "Test City",
              "state": "TS",
              "marina": "Test Marina"
            }
            """;

        MockMultipartFile boatPart = new MockMultipartFile(
                "boat",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                boatJson.getBytes()
        );

        MvcResult boatCreateResult = mockMvc.perform(
                        multipart("/api/boats")
                                .file(boatPart)
                                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andReturn();

        String boatResponse = boatCreateResult.getResponse().getContentAsString();
        Long boatId = ((Number) JsonPath.read(boatResponse, "$.id")).longValue();
        this.testBoat = boatRepository.findById(boatId).orElseThrow();

        // 3. Create availability for the boat
        LocalDateTime availabilityStart = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        LocalDateTime availabilityEnd = availabilityStart.plusDays(30);
        String startDateStr = availabilityStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endDateStr = availabilityEnd.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String availabilityJson = String.format("""
            {
                "startDate": "%s",
                "endDate": "%s",
                "pricePerHour": 250.00
            }
            """, startDateStr, endDateStr);

        MvcResult availabilityResult = mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availabilityJson))
                .andExpect(status().isOk())
                .andReturn();

        String availabilityResponse = availabilityResult.getResponse().getContentAsString();
        Long availabilityId = ((Number) JsonPath.read(availabilityResponse, "$.id")).longValue();
        this.testAvailability = boatAvailabilityRepository.findById(availabilityId).orElseThrow();

        // 4. Create a booking (with approved card) to use in payment tests
        this.startDate = availabilityStart.plusDays(1).withHour(10).withMinute(0);
        this.endDate = startDate.plusHours(4);
        this.bookingStartStr = startDate.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.bookingEndStr = startDate.plusHours(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String bookingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, bookingStartStr, bookingEndStr, cardToJson(approvedCard()));

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String bookingResponse = bookingResult.getResponse().getContentAsString();
        Long bookingId = ((Number) JsonPath.read(bookingResponse, "$.id")).longValue();
        this.testBooking = bookingRepository.findById(bookingId).orElseThrow();
    }

    @AfterEach
    void cleanup() {
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        boatAvailabilityRepository.deleteAll();
        boatRepository.deleteAll();
        userRepository.deleteAll();
        clearCurrentUser(); // clear security context thread locals
    }

    // -----------------------------------------------------------------
    //  POST /api/payments/booking
    // -----------------------------------------------------------------

    @Test
    @DisplayName("You should be able to successfully process your booking payment.")
    void processBookingPayment_Success() throws Exception {
        // given
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("250.00"));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setBookingId(testBooking.getId());
        request.setDescription("Test booking payment");
        request.setInstallments(1);
        request.setMockCardData(approvedCard());

        String requestJson = objectMapper.writeValueAsString(request);

        // when & then
        MvcResult result = mockMvc.perform(post("/api/payments/booking")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value(PaymentStatus.CONFIRMED.name()))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.bookingId").value(testBooking.getId()))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        String transactionId = JsonPath.read(responseJson, "$.transactionId");

        // verify that a payment was persisted
        Payment savedPayment = paymentRepository.findByTransactionId(transactionId).orElse(null);
        assertThat(savedPayment).isNotNull();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(savedPayment.getBooking().getId()).isEqualTo(testBooking.getId());
    }

    @Test
    @DisplayName("It should return 400 when the request is invalid (validation error).")
    void processBookingPayment_ValidationError() throws Exception {
        // given: missing amount
        String invalidJson = """
            {
                "paymentMethod": "CREDIT_CARD",
                "bookingId": 1
            }
            """;

        mockMvc.perform(post("/api/payments/booking")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("It should return a 401 error when not authenticated.")
    void processBookingPayment_Unauthorized() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("250.00"));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setBookingId(testBooking.getId());
        request.setMockCardData(approvedCard());

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/payments/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("It must process the declined card payment and return success (failure status).")
    void processBookingPayment_DeclinedCard() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("250.00"));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setBookingId(testBooking.getId());
        request.setDescription("Test declined payment");
        request.setInstallments(1);
        request.setMockCardData(declinedCard());

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/payments/booking")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value(PaymentStatus.CANCELLED.name()))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.bookingId").value(testBooking.getId()));
    }

    @Test
    @DisplayName("You must process the pending card payment.")
    void processBookingPayment_PendingCard() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("250.00"));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setBookingId(testBooking.getId());
        request.setDescription("Test pending payment");
        request.setInstallments(1);
        request.setMockCardData(pendingCard());

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/payments/booking")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value(PaymentStatus.PENDING.name()))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.bookingId").value(testBooking.getId()));
    }

    // -----------------------------------------------------------------
    //  POST /api/payments/direct
    // -----------------------------------------------------------------

    @Test
    @DisplayName("You should be able to successfully process the direct payment.")
    void processDirectPayment_Success() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("150.00"));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setBoatId(testBoat.getId());
        request.setDescription("Test direct payment");
        request.setInstallments(1);
        request.setMockCardData(approvedCard());

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/payments/direct")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value(PaymentStatus.CONFIRMED.name()))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.boatId").value(testBoat.getId()));
    }

    @Test
    @DisplayName("You must process the payment directly using PIX.")
    void processDirectPayment_Pix() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("80.00"));
        request.setPaymentMethod(PaymentMethod.PIX);
        request.setBoatId(testBoat.getId());
        request.setDescription("Test PIX payment");
        request.setMockCardData(null); // no card data for PIX

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/payments/direct")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value(PaymentStatus.CONFIRMED.name()))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.boatId").value(testBoat.getId()));
    }

    @Test
    @DisplayName("You must process payment directly with Bank slip.")
    void processDirectPayment_Boleto() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("120.00"));
        request.setPaymentMethod(PaymentMethod.BOLETO);
        request.setBoatId(testBoat.getId());
        request.setDescription("Test payment by bank slip");
        request.setMockCardData(null);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/payments/direct")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value(PaymentStatus.CONFIRMED.name()))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.boatId").value(testBoat.getId()));
    }

    @Test
    void processDirectPayment_shouldReturn400WhenBothBookingIDAndBoatIdProvided() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("150.00"));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setBookingId(testBooking.getId());  // both present
        request.setBoatId(testBoat.getId());
        request.setMockCardData(approvedCard());

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/payments/direct")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processDirectPayment_shouldReturn400WhenBothBookingIdAndBoatIdNotProvided() throws Exception {

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setAmount(new BigDecimal("150.00"));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setBookingId(null);
        request.setBoatId(null);
        request.setMockCardData(approvedCard());

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/payments/direct")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------
    //  GET /api/payments/transaction/{transactionId}
    // -----------------------------------------------------------------

    @Test
    @DisplayName("It should return a 404 (stubbed)")
    void getPaymentByTransactionId_NotFound() throws Exception {
        mockMvc.perform(get("/api/payments/transaction/TX123")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("It should return a 401 error when not authenticated.")
    void getPaymentByTransactionId_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/payments/transaction/TX123"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------
    //  GET /api/payments/history
    // -----------------------------------------------------------------

    @Test
    @DisplayName("It should return 200 with an empty (stubbed) body.")
    void getPaymentHistory_Success() throws Exception {
        mockMvc.perform(get("/api/payments/history")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("It should return a 401 error when not authenticated.")
    void getPaymentHistory_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/payments/history"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------
    //  POST /api/payments/webhook/pagseguro
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Webhook - must process valid notification and update payment status.")
    void handlePaymentWebhook_ValidSignatureAndTransaction_ShouldReturn200() throws Exception {
        // 1. Create a reservation with PIX.
        String newBookingStartStr = startDate.plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String newBookingEndStr = startDate.plusDays(5).plusHours(6).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String bookingRequestJson = String.format("""
        {
            "boatId": %d,
            "startDate": "%s",
            "endDate": "%s",
            "paymentMethod": "PIX",
            "mockCardData": null
        }
        """, testBoat.getId(), newBookingStartStr, newBookingEndStr);

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isCreated())
                .andReturn();

        // 2. Extracts the reservation ID and retrieves the transaction ID of the payment via the repository.

        String bookingResponse = bookingResult.getResponse().getContentAsString();
        Long bookingId = ((Number) JsonPath.read(bookingResponse, "$.id")).longValue();

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new AssertionError("Payment not found for the reservation. " + bookingId));
        String transactionId = payment.getTransactionId();

        // 3. Monta o payload do webhook
        String webhookPayload = String.format("""
        {
            "notificationCode": "NC-%s",
            "notificationType": "transaction",
            "code": "%s",
            "status": 3
        }
        """, System.currentTimeMillis(), transactionId);

        // 4. Calculate the HMAC-SHA256 signature.
        String secret = "chave_teste_sandbox";
        String signature = hmacSha256Base64(secret, webhookPayload);

        // 5. Send the webhook
        mockMvc.perform(post("/api/payments/webhook/pagseguro")
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk());

        // 6. Check the status update.
        Payment updatedPayment = paymentRepository.findByTransactionId(transactionId).orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);

    }

    // -----------------------------------------------------------------
    //  GET /api/payments/health
    // -----------------------------------------------------------------

    @Test
    @DisplayName("It should return 200 when the service is healthy.")
    void healthCheck_Healthy() throws Exception {
        mockMvc.perform(get("/api/payments/health")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Payment service is healthy"));
    }

    // -----------------------------------------------------------------
    //  Test Security Configuration (adapted to provide Long userId)
    // -----------------------------------------------------------------

    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> currentUserRoles = new ThreadLocal<>();

    public static void setCurrentUser(Long userId, String role) {
        currentUserId.set(userId);
        currentUserRoles.set(List.of(role));
    }

    public static void setCurrentUser(Long userId, List<String> roles) {
        currentUserId.set(userId);
        currentUserRoles.set(roles);
    }

    public static void clearCurrentUser() {
        currentUserId.remove();
        currentUserRoles.remove();
    }

    @TestConfiguration
    @Slf4j
    static class TestSecurityConfig {

        @Autowired
        private UserRepository userRepository;

        @Bean
        @Primary
        public JwtAuthenticationFilter testJwtAuthenticationFilter() {
            return new JwtAuthenticationFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain)
                        throws ServletException, IOException {

                    // 1. If test user ID is explicitly set, use it (simulated authentication)
                    Long testUserId = currentUserId.get();
                    List<String> testRoles = currentUserRoles.get();

                    if (testUserId != null) {
                        List<GrantedAuthority> authorities = testRoles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .map(a -> (GrantedAuthority) a)
                                .collect(Collectors.toList());

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(testUserId, null, authorities);

                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);

                        filterChain.doFilter(request, response);
                        return;
                    }

                    // 2. Otherwise, try to authenticate with real JWT token
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            // Extract email from token (simplified, works with our JwtService)
                            String email = extractEmailFromToken(token);
                            if (email != null) {
                                User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User not found"));
                                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(user.getId(), null, authorities);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to authenticate with JWT token", e);
                        }
                    }

                    filterChain.doFilter(request, response);
                }

                private String extractEmailFromToken(String token) {
                    try {
                        String[] chunks = token.split("\\.");
                        if (chunks.length > 1) {
                            String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
                            // Expecting payload to contain "sub": "email@example.com"
                            int subIndex = payload.indexOf("\"sub\":\"");
                            if (subIndex != -1) {
                                int start = subIndex + 7;
                                int end = payload.indexOf("\"", start);
                                return payload.substring(start, end);
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    return null;
                }
            };
        }
    }
}