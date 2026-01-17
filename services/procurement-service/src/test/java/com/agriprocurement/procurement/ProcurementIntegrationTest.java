package com.agriprocurement.procurement;

import com.agriprocurement.common.domain.valueobject.Money;
import com.agriprocurement.common.domain.valueobject.Quantity;
import com.agriprocurement.procurement.application.CreateProcurementRequest;
import com.agriprocurement.procurement.application.ProcurementResponse;
import com.agriprocurement.procurement.application.SubmitBidRequest;
import com.agriprocurement.procurement.domain.Procurement;
import com.agriprocurement.procurement.domain.ProcurementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class ProcurementIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProcurementRepository procurementRepository;

    @BeforeEach
    void setUp() {
        procurementRepository.deleteAll();
    }

    @Test
    void shouldCreateProcurement() throws Exception {
        // Given
        CreateProcurementRequest request = new CreateProcurementRequest(
            "Agricultural Equipment Procurement",
            "Procurement for farming equipment needed for the season",
            BigDecimal.valueOf(100),
            Quantity.Unit.PIECE,
            BigDecimal.valueOf(50000),
            "USD",
            LocalDateTime.now().plusDays(30),
            UUID.randomUUID()
        );

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/procurements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value(request.title()))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        assertThat(responseJson).contains("Procurement created successfully");
    }

    @Test
    void shouldValidateRequiredFieldsWhenCreatingProcurement() throws Exception {
        // Given - invalid request with missing fields
        CreateProcurementRequest request = new CreateProcurementRequest(
            null, // Missing title
            "Description",
            BigDecimal.valueOf(100),
            Quantity.Unit.PIECE,
            BigDecimal.valueOf(50000),
            "USD",
            LocalDateTime.now().plusDays(30),
            UUID.randomUUID()
        );

        // When & Then
        mockMvc.perform(post("/api/procurements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldSubmitBid() throws Exception {
        // Given - Create a procurement first
        Procurement procurement = new Procurement(
            "Test Procurement",
            "Test Description for procurement",
            Quantity.of(100, Quantity.Unit.PIECE),
            Money.of(50000, "USD"),
            LocalDateTime.now().plusDays(30),
            UUID.randomUUID()
        );
        procurement.publish();
        procurement.openBidding();
        procurement = procurementRepository.save(procurement);

        SubmitBidRequest bidRequest = new SubmitBidRequest(
            procurement.getId(),
            UUID.randomUUID(),
            BigDecimal.valueOf(45000),
            "USD",
            "Competitive bid with quality guarantee"
        );

        // When & Then
        mockMvc.perform(post("/api/procurements/" + procurement.getId() + "/bids")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bidRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.bidCount").value(1));
    }

    @Test
    void shouldGetProcurement() throws Exception {
        // Given
        Procurement procurement = new Procurement(
            "Test Procurement",
            "Test Description for procurement",
            Quantity.of(100, Quantity.Unit.PIECE),
            Money.of(50000, "USD"),
            LocalDateTime.now().plusDays(30),
            UUID.randomUUID()
        );
        procurement = procurementRepository.save(procurement);

        // When & Then
        mockMvc.perform(get("/api/procurements/" + procurement.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(procurement.getId().toString()))
            .andExpect(jsonPath("$.data.title").value("Test Procurement"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentProcurement() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/api/procurements/" + nonExistentId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldListProcurements() throws Exception {
        // Given
        Procurement procurement1 = new Procurement(
            "Procurement 1",
            "Description 1 for procurement",
            Quantity.of(100, Quantity.Unit.PIECE),
            Money.of(50000, "USD"),
            LocalDateTime.now().plusDays(30),
            UUID.randomUUID()
        );
        Procurement procurement2 = new Procurement(
            "Procurement 2",
            "Description 2 for procurement",
            Quantity.of(200, Quantity.Unit.KG),
            Money.of(75000, "USD"),
            LocalDateTime.now().plusDays(45),
            UUID.randomUUID()
        );
        procurementRepository.save(procurement1);
        procurementRepository.save(procurement2);

        // When & Then
        mockMvc.perform(get("/api/procurements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void shouldPublishProcurement() throws Exception {
        // Given
        Procurement procurement = new Procurement(
            "Test Procurement",
            "Test Description for procurement",
            Quantity.of(100, Quantity.Unit.PIECE),
            Money.of(50000, "USD"),
            LocalDateTime.now().plusDays(30),
            UUID.randomUUID()
        );
        procurement = procurementRepository.save(procurement);

        // When & Then
        mockMvc.perform(put("/api/procurements/" + procurement.getId() + "/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Procurement published successfully"));

        // Verify status changed
        Procurement updated = procurementRepository.findById(procurement.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Procurement.ProcurementStatus.BIDDING_OPEN);
    }

    @Test
    void shouldAwardProcurement() throws Exception {
        // Given - Create procurement with bids
        Procurement procurement = new Procurement(
            "Test Procurement",
            "Test Description for procurement",
            Quantity.of(100, Quantity.Unit.PIECE),
            Money.of(50000, "USD"),
            LocalDateTime.now().plusDays(30),
            UUID.randomUUID()
        );
        procurement.publish();
        procurement.openBidding();
        var bid = procurement.addBid(UUID.randomUUID(), Money.of(45000, "USD"));
        procurement.closeBidding();
        procurement = procurementRepository.save(procurement);

        // When & Then
        mockMvc.perform(put("/api/procurements/" + procurement.getId() + "/award")
                .param("bidId", bid.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("AWARDED"));
    }

    @Test
    void shouldCancelProcurement() throws Exception {
        // Given
        Procurement procurement = new Procurement(
            "Test Procurement",
            "Test Description for procurement",
            Quantity.of(100, Quantity.Unit.PIECE),
            Money.of(50000, "USD"),
            LocalDateTime.now().plusDays(30),
            UUID.randomUUID()
        );
        procurement = procurementRepository.save(procurement);

        // When & Then
        mockMvc.perform(put("/api/procurements/" + procurement.getId() + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify status changed
        Procurement updated = procurementRepository.findById(procurement.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Procurement.ProcurementStatus.CANCELLED);
    }

    @Test
    void shouldListActiveProcurements() throws Exception {
        // Given
        Procurement procurement1 = new Procurement(
            "Active Procurement",
            "Description for active procurement",
            Quantity.of(100, Quantity.Unit.PIECE),
            Money.of(50000, "USD"),
            LocalDateTime.now().plusDays(30),
            UUID.randomUUID()
        );
        procurement1.publish();
        
        Procurement procurement2 = new Procurement(
            "Draft Procurement",
            "Description for draft procurement",
            Quantity.of(200, Quantity.Unit.KG),
            Money.of(75000, "USD"),
            LocalDateTime.now().plusDays(45),
            UUID.randomUUID()
        );
        
        procurementRepository.save(procurement1);
        procurementRepository.save(procurement2);

        // When & Then
        mockMvc.perform(get("/api/procurements")
                .param("activeOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldListProcurementsByBuyer() throws Exception {
        // Given
        UUID buyerId = UUID.randomUUID();
        UUID otherBuyerId = UUID.randomUUID();
        
        Procurement procurement1 = new Procurement(
            "Buyer Procurement 1",
            "Description for buyer procurement 1",
            Quantity.of(100, Quantity.Unit.PIECE),
            Money.of(50000, "USD"),
            LocalDateTime.now().plusDays(30),
            buyerId
        );
        
        Procurement procurement2 = new Procurement(
            "Other Buyer Procurement",
            "Description for other buyer procurement",
            Quantity.of(200, Quantity.Unit.KG),
            Money.of(75000, "USD"),
            LocalDateTime.now().plusDays(45),
            otherBuyerId
        );
        
        procurementRepository.save(procurement1);
        procurementRepository.save(procurement2);

        // When & Then
        mockMvc.perform(get("/api/procurements")
                .param("buyerId", buyerId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].buyerId").value(buyerId.toString()));
    }
}
