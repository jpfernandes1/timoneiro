package com.jompastech.backend.model.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a PagSeguro webhook notification.
 * Only includes fields needed for payment status update.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PagSeguroWebhookDTO(
        @JsonProperty("notificationCode") String notificationCode,
        @JsonProperty("notificationType") String notificationType,
        @JsonProperty("code") String transactionCode,   // gateway transaction ID
        @JsonProperty("reference") String reference,    // optional: our internal booking ID or payment ID
        @JsonProperty("status") Integer status          // PagSeguro status code (3 = paid, etc.)
) {}