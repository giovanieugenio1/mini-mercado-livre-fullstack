package com.miniml.payment.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String field, String value) {
        super("Pagamento não encontrado: " + field + "=" + value);
    }
}
