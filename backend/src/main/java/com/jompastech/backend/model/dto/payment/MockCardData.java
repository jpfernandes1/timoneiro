package com.jompastech.backend.model.dto.payment;

public class MockCardData {
    private String cardNumber;
    private String holderName;
    private String expirationDate;
    private String cvv;

    // Constructor
    public MockCardData(String cardNumber, String holderName, String expirationDate, String cvv) {
        this.cardNumber = cardNumber;
        this.holderName = holderName;
        this.expirationDate = expirationDate;
        this.cvv = cvv;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getCvv() {
        return cvv;
    }
}