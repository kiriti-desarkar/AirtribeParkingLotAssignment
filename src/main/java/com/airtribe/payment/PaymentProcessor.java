package com.airtribe.payment;

public interface PaymentProcessor {
    boolean processPayment(double amount);
}
