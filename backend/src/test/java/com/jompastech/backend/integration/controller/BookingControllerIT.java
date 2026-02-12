package com.jompastech.backend.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jompastech.backend.model.dto.payment.MockCardData;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.model.enums.BookingStatus;
import com.jompastech.backend.repository.*;
import com.jompastech.backend.security.dto.AuthRequestDTO;
import com.jompastech.backend.security.filter.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;

import static com.jompastech.backend.integration.controller.util.CpfGenerator.generateValidCpf;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingControllerIT {

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

    private String jwtToken;
    private Long boatId;
    private Long availabilityId;
    private String userEmail;
    private User testUser;
    private Boat testBoat;
    private BoatAvailability testAvailability;

    // Datas para os testes (sempre no futuro)
    private final LocalDateTime startDate = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
    private final LocalDateTime endDate = startDate.plusHours(4);
    String bookingStartStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    String bookingEndStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    // Availability
    LocalDateTime availabilityStart = startDate.minusDays(1);
    LocalDateTime availabilityEnd = endDate.plusDays(2);

    String startDateStr = availabilityStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    String endDateStr = availabilityEnd.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    // JSON para criar barco (sem imagens)
    private final String boatJson = """
        {
          "name": "Boat for Booking Tests",
          "description": "Boat used in booking integration tests",
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

    @BeforeEach
    void setup() throws Exception {
        // 1. Cria um usuário único e obtém token JWT
        this.userEmail = "booking_test_" + System.nanoTime() + "@boat.com";
        String validCpf = generateValidCpf();

        String userJson = String.format("""
            {
              "name": "Booking Test User",
              "email": "%s",
              "password": "asd@12345",
              "cpf": "%s",
              "phone": "11999999999"
            }
            """, userEmail, validCpf);

        // Registro
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());

        // Login
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

        // 2. Cria um barco (via API, autenticado)
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
        this.boatId = ((Number) JsonPath.read(boatResponse, "$.id")).longValue();
        this.testBoat = boatRepository.findById(boatId).orElseThrow();

        // 3. Cria uma disponibilidade para o barco (via API, autenticado)
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
        this.availabilityId = ((Number) JsonPath.read(availabilityResponse, "$.id")).longValue();
        this.testAvailability = boatAvailabilityRepository.findById(availabilityId).orElseThrow();
    }

    @AfterEach
    void cleanup() {
        // Ordem correta respeitando as FKs: bookings → availabilities → boats → users
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        boatAvailabilityRepository.deleteAll();
        boatRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Cria um MockCardData com dados de cartão válidos (pagamento aprovado)
    private MockCardData validCard() {
        MockCardData card = new MockCardData(
                "4111111111111111",
                "Jose da Silva",
                "03/2026",
                "123"
        );
        return card;
    }

    // Cria um MockCardData que força recusa do pagamento
    private MockCardData refusedCard() {
        MockCardData card = new MockCardData(
                "4222222222222222",
                "Jose da Silva",
                "12/2026",
                "123"
        );
        return card;
    }

    // Converte MockCardData para JSON manualmente (simples)
    private String cardToJson(MockCardData card) throws Exception {
        return objectMapper.writeValueAsString(card);
    }

    // ----------------------------------------------------------------
    //  POST /api/bookings
    // ----------------------------------------------------------------

    @Test
    void shouldCreateBookingSuccessfully() throws Exception {
        String bookingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, bookingStartStr, bookingEndStr, cardToJson(validCard()));

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.boat.id").value(boatId))
                .andExpect(jsonPath("$.user.id").value(testUser.getId()))
                .andExpect(jsonPath("$.startDate").value(bookingStartStr))
                .andExpect(jsonPath("$.endDate").value(bookingEndStr))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalPrice").value(1000.00))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Long bookingId = ((Number) JsonPath.read(response, "$.id")).longValue();

        // Verifica no banco
        Booking saved = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, saved.getStatus());
        assertEquals(new BigDecimal("1000.00"), saved.getTotalPrice());
    }

    @Test
    void shouldReturn400WhenInvalidBookingRequest() throws Exception {
        // Data de início depois do fim
        String invalidDatesJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, endDateStr, startDateStr, cardToJson(validCard()));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidDatesJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WhenNoTokenProvided() throws Exception {
        String bookingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, startDateStr, endDateStr, cardToJson(validCard()));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn404WhenBoatNotFound() throws Exception {
        Long nonExistentBoatId = 99999L;

        String bookingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, nonExistentBoatId, bookingStartStr, bookingEndStr, cardToJson(validCard()));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenBoatNotAvailable() throws Exception {
        // 1. Cria um booking com sucesso
        String bookingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, bookingStartStr, bookingEndStr, cardToJson(validCard()));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isCreated());

        // 2. Tenta criar outro booking com período sobreposto (dentro do mesmo horário)
        String overlappingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, startDateStr, endDateStr, cardToJson(validCard()));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingRequestJson))
                .andExpect(status().isConflict());   // 409 Conflict
    }

    @Test
    void shouldReturn402WhenPaymentFails() throws Exception {
        String bookingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, bookingStartStr, bookingEndStr, cardToJson(refusedCard()));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isPaymentRequired()); // 402
    }

    // ----------------------------------------------------------------
    //  GET /api/bookings/my-bookings
    // ----------------------------------------------------------------

    @Test
    void shouldGetMyBookings() throws Exception {

        // Cria segunda disponibilidade
        String nextDayAvailabilityStartStr = availabilityStart.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String nextDayAvailabilityEndStr = availabilityEnd.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String availabilityJson = String.format("""
    {
        "startDate": "%s",
        "endDate": "%s",
        "pricePerHour": 250.00
    }
    """, nextDayAvailabilityStartStr, nextDayAvailabilityEndStr);

        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availabilityJson))
                .andExpect(status().isOk());

        // Cria dois bookings para o usuário
        createBookingForUser(boatId, startDate, endDate, validCard());
        createBookingForUser(boatId, startDate.plusDays(1), endDate.plusDays(1), validCard());

        mockMvc.perform(get("/api/bookings/my-bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].user.id").value(testUser.getId()))
                .andExpect(jsonPath("$.content[1].user.id").value(testUser.getId()));
    }

    @Test
    void shouldReturn401WhenGetMyBookingsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/bookings/my-bookings"))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    //  POST /api/bookings/{bookingId}/cancel (não implementado)
    // ----------------------------------------------------------------

    @Test
    void shouldReturn501ForCancelBooking() throws Exception {
        // Cria um booking para ter um ID válido
        Long bookingId = createBookingForUser(boatId, startDate, endDate, validCard());

        mockMvc.perform(post("/api/bookings/{bookingId}/cancel", bookingId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotImplemented());  // 501
    }

    @Test
    void shouldGetBookingByIdWhenUserIsBookingOwner() throws Exception {
        Long bookingId = createBookingForUser(boatId, startDate, endDate, validCard());

        mockMvc.perform(get("/api/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.user.id").value(testUser.getId()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldGetBookingByIdWhenUserIsBoatOwner() throws Exception {
        // Cria um segundo usuário (inquilino)
        String renterEmail = "renter_" + System.nanoTime() + "@boat.com";
        createUser(renterEmail);
        String renterToken = doLogin(renterEmail, "asd@12345");

        // Inquilino faz a reserva
        Long bookingId = createBookingForUserWithToken(boatId, startDate, endDate, validCard(), renterToken);

        // Dono do barco (testUser) tenta acessar
        mockMvc.perform(get("/api/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.user.email").value(renterEmail));
    }

    @Test
    void shouldReturn404WhenBookingNotFound() throws Exception {
        mockMvc.perform(get("/api/bookings/{bookingId}", 99999L)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenUserIsNotAuthorized() throws Exception {
        // Cria um terceiro usuário (intruso)
        String intruderEmail = "intruder_" + System.nanoTime() + "@boat.com";
        createUser(intruderEmail);
        String intruderToken = doLogin(intruderEmail, "asd@12345");

        // Cria reserva com o usuário padrão (dono da reserva)
        Long bookingId = createBookingForUser(boatId, startDate, endDate, validCard());

        // Intruso tenta acessar
        mockMvc.perform(get("/api/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenAuthenticatedUserNotFound() throws Exception {
        // 1. Cria um usuário temporário (sem barco, sem reservas)
        String tempEmail = "temp_" + System.nanoTime() + "@boat.com";
        String validCpf = generateValidCpf();
        String userJson = String.format("""
        {
            "name": "Temp User",
            "email": "%s",
            "password": "asd@12345",
            "cpf": "%s",
            "phone": "11999999999"
        }
        """, tempEmail, validCpf);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());

        // 2. Login para obter o token JWT
        String tempToken = doLogin(tempEmail, "asd@12345");

        // 3. Deleta o usuário do banco (não há FK pendentes)
        User tempUser = userRepository.findByEmail(tempEmail).orElseThrow();
        userRepository.delete(tempUser);
        userRepository.flush();

        // 4. Requisição com token válido, mas usuário inexistente → 404
        mockMvc.perform(get("/api/bookings/my-bookings")
                        .header("Authorization", "Bearer " + tempToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with email: " + tempEmail));
    }

    @Test
    void shouldGetMyBookingsWithStatusFilter() throws Exception {
        // Cria uma reserva CONFIRMED (cartão aprovado)
        Long bookingId = createBookingForUser(boatId, startDate, endDate, validCard());

        mockMvc.perform(get("/api/bookings/my-bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(bookingId))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    // ----------------------------------------------------------------
    //  MÉTODO AUXILIAR: Cria um booking e retorna o ID
    // ----------------------------------------------------------------

    /**
    * Realiza login e retorna o token JWT.
    */
    private String doLogin(String email, String password) throws Exception {
        AuthRequestDTO authRequest = new AuthRequestDTO();
        authRequest.setEmail(email);
        authRequest.setPassword(password);
        String authResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(authResponse).get("token").asText();
    }

    /**
     * Cria um novo usuário com email e senha padrão.
     */
    private void createUser(String email) throws Exception {
        String validCpf = generateValidCpf();
        String userJson = String.format("""
        {
            "name": "Test User",
            "email": "%s",
            "password": "asd@12345",
            "cpf": "%s",
            "phone": "11999999999"
        }
        """, email, validCpf);
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());
    }

    /**
     * Cria uma reserva usando um token específico (útil para cenários com múltiplos usuários).
     */
    private Long createBookingForUserWithToken(Long boatId, LocalDateTime start, LocalDateTime end,
                                               MockCardData card, String token) throws Exception {
        String startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String requestJson = String.format("""
        {
            "boatId": %d,
            "startDate": "%s",
            "endDate": "%s",
            "paymentMethod": "CREDIT_CARD",
            "mockCardData": %s
        }
        """, boatId, startStr, endStr, cardToJson(card));

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private Long createBookingForUser(Long boatId, LocalDateTime start, LocalDateTime end,
                                      MockCardData card) throws Exception {
        String startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String requestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, startStr, endStr, cardToJson(card));

        MvcResult result;
        if (card.getCardNumber().equals("4000000000000002")) {
            // Pagamento recusado → retorna 402, mas o booking é criado e cancelado
            result = mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isPaymentRequired())
                    .andReturn();
        } else {
            result = mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andReturn();
        }

        String response = result.getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    // ==============================================
    // TEST CONFIGURATION FOR SECURITY (mesma do BoatAvailabilityControllerIT)
    // ==============================================
    private static final ThreadLocal<String> currentUserEmail = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> currentUserRoles = new ThreadLocal<>();

    public static void setCurrentUser(String email, String role) {
        currentUserEmail.set(email);
        currentUserRoles.set(List.of(role));
    }

    public static void setCurrentUser(String email, List<String> roles) {
        currentUserEmail.set(email);
        currentUserRoles.set(roles);
    }

    public static void clearCurrentUser() {
        currentUserEmail.remove();
        currentUserRoles.remove();
    }

    @TestConfiguration
    @Slf4j
    static class TestSecurityConfig {

        @Bean
        @Primary
        public JwtAuthenticationFilter testJwtAuthenticationFilter() {
            return new JwtAuthenticationFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain)
                        throws ServletException, IOException {

                    // 1. Se um contexto de teste foi explicitamente definido, usa-o
                    String testEmail = currentUserEmail.get();
                    List<String> testRoles = currentUserRoles.get();

                    if (testEmail != null) {
                        List<GrantedAuthority> authorities = testRoles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .map(a -> (GrantedAuthority) a)
                                .toList();

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(testEmail, null, authorities);

                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);

                        filterChain.doFilter(request, response);
                        return;
                    }

                    // 2. Caso contrário, tenta extrair o email do token (comportamento real)
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            // Aqui você pode usar seu validador real, ou simplificar:
                            // Exemplo: extrair o subject do token sem validação (para testes)
                            // Para simplificar, vamos assumir que qualquer token é válido e o email está no subject
                            String email = extractEmailFromToken(token); // método auxiliar fictício
                            if (email != null) {
                                // Usuário comum para testes (ROLE_USER)
                                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        } catch (Exception e) {
                            log.warn("Falha ao processar token no teste, usando fallback");
                        }
                    }

                    filterChain.doFilter(request, response);
                }

                private String extractEmailFromToken(String token) {
                    // Implementação simplificada para testes - extrai o subject do JWT sem verificar assinatura
                    try {
                        String[] chunks = token.split("\\.");
                        if (chunks.length > 1) {
                            String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
                            // Procura pelo campo "sub"
                            int subIndex = payload.indexOf("\"sub\":\"");
                            if (subIndex != -1) {
                                int start = subIndex + 7;
                                int end = payload.indexOf("\"", start);
                                return payload.substring(start, end);
                            }
                        }
                    } catch (Exception e) {
                        // ignora
                    }
                    return null;
                }
            };
        }
    }

}