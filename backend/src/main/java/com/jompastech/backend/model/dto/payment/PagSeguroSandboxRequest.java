package com.jompastech.backend.model.dto.payment;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for PagSeguro sandbox API integration.
 * Models the complete contract for payment processing requests.
 *
 * Design Decisions:
 * - Exact mapping to PagSeguro API v2 structure for seamless integration
 * - Nested static classes represent complex JSON hierarchies in API contract
 * - Supports Brazilian payment methods (PIX, Boleto) alongside international cards
 * - Manual getters/setters for explicit serialization control and API compatibility
 *
 * Trade-offs Accepted:
 * - No builder pattern to maintain clear 1:1 mapping with PagSeguro API schema
 * - Some optional PagSeguro fields omitted for initial implementation simplicity
 * - Validation delegated to service layer rather than DTO annotations
 * - Portuguese language hardcoded for Brazilian market focus
 */
@Data
public class PagSeguroSandboxRequest {

    /**
     * Merchant reference ID for order tracking and reconciliation.
     * Typically follows format: "timoneiro_booking_{timestamp}"
     */
    private String referenceId;

    /**
     * Webhook URL for asynchronous payment status notifications.
     * Essential for PIX and boleto payments with delayed confirmation.
     */
    private String notificationUrl;

    /**
     * Integration configuration for merchant settings.
     */
    private Integration integration;

    /**
     * Order details including currency, items, and pricing.
     */
    private Order order;

    /**
     * Payment charge configuration based on selected method.
     */
    private Charge charge;

    /**
     * Payer information for customer identification.
     */
    private Payer payer;

    /**
     * Factory method to create a basic request structure.
     *
     * @param referenceId Unique identifier for the transaction
     * @param notificationUrl Webhook URL for status updates
     * @return Basic request structure with integration setup
     */
    public static PagSeguroSandboxRequest createBasic(String referenceId, String notificationUrl) {
        PagSeguroSandboxRequest request = new PagSeguroSandboxRequest();
        request.setReferenceId(referenceId);
        request.setNotificationUrl(notificationUrl);

        Integration integration = new Integration();
        integration.setReference(referenceId);
        integration.setNotificationUrl(notificationUrl);
        integration.setLanguage("pt_BR"); // Brazilian Portuguese

        request.setIntegration(integration);
        return request;
    }

    /**
     * Integration configuration inner class.
     * Maps to PagSeguro's integration object for merchant settings.
     */
    @Data
    public static class Integration {
        private String reference;
        private String notificationUrl;
        private String language = "pt_BR"; // Default to Brazilian Portuguese
    }

    /**
     * Order details inner class.
     * Contains currency and item information for the transaction.
     */
    @Data
    public static class Order {
        private String currency = "BRL"; // Brazilian Real
        private List<OrderItem> items;

        /**
         * Calculates total order amount from items.
         *
         * @return Total order value or BigDecimal.ZERO if no items
         */
        public BigDecimal getTotalAmount() {
            if (items == null || items.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return items.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /**
         * Adds a single item to the order.
         *
         * @param description Item description
         * @param unitPrice Price per unit
         * @param quantity Item quantity (defaults to 1)
         */
        public void addItem(String description, BigDecimal unitPrice, Integer quantity) {
            OrderItem item = new OrderItem();
            item.setDescription(description);
            item.setUnitPrice(unitPrice);
            item.setQuantity(quantity != null ? quantity : 1);

            if (this.items == null) {
                this.items = List.of(item);
            } else {
                this.items = List.of(item); // Replace existing items for simplicity
            }
        }
    }

    /**
     * Order item inner class.
     * Represents individual products or services in the order.
     */
    @Data
    public static class OrderItem {
        private Integer quantity = 1;
        private String description;
        private BigDecimal unitPrice;

        /**
         * Validates that the order item has required fields.
         *
         * @return true if item has description and positive unit price
         */
        public boolean isValid() {
            return description != null && !description.trim().isEmpty() &&
                    unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0 &&
                    quantity != null && quantity > 0;
        }

        /**
         * Calculates total price for this item line.
         *
         * @return quantity × unitPrice
         */
        public BigDecimal getTotalPrice() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /**
     * Charge configuration inner class.
     * Defines payment method and country-specific settings.
     */
    @Data
    public static class Charge {
        private String country = "BR"; // Brazil
        private String type; // CREDIT_CARD, PIX, BOLETO

        /**
         * Validates charge configuration.
         *
         * @return true if type is specified and country is BR
         */
        public boolean isValid() {
            return type != null && !type.trim().isEmpty() && "BR".equals(country);
        }

        /**
         * Supported payment types in the Brazilian market.
         */
        public enum PaymentType {
            CREDIT_CARD, PIX, BOLETO
        }
    }

    /**
     * Payer information inner class.
     * Contains customer identification data.
     */
    @Data
    public static class Payer {
        private String email;
        private String firstName;
        private String lastName;
        private String taxId; // CPF or CNPJ

        /**
         * Gets full name by combining first and last name.
         *
         * @return Full name or null if both names are missing
         */
        public String getFullName() {
            if (firstName == null && lastName == null) return null;
            if (firstName == null) return lastName;
            if (lastName == null) return firstName;
            return firstName + " " + lastName;
        }

        /**
         * Validates that payer has minimum required information.
         *
         * @return true if email is present and valid
         */
        public boolean isValid() {
            return email != null && email.contains("@") && email.contains(".");
        }
    }

    /**
     * Validates the complete request structure.
     *
     * @return true if all required components are present and valid
     */
    public boolean isValid() {
        return referenceId != null && !referenceId.trim().isEmpty() &&
                notificationUrl != null && !notificationUrl.trim().isEmpty() &&
                integration != null &&
                order != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0 &&
                charge != null && charge.isValid() &&
                payer != null && payer.isValid();
    }

    /**
     * Gets a descriptive string for logging (without sensitive data).
     *
     * @return Safe log representation
     */
    public String toLogString() {
        return String.format(
                "PagSeguroRequest{referenceId='%s', amount=%s, currency=%s, type=%s}",
                referenceId,
                order != null ? order.getTotalAmount() : "null",
                order != null ? order.getCurrency() : "null",
                charge != null ? charge.getType() : "null"
        );
    }
}