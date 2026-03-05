package com.miniml.shipping.exception;

import java.util.UUID;

public class ShippingNotFoundException extends RuntimeException {
    public ShippingNotFoundException(UUID orderId) {
        super("Envio não encontrado para o pedido: " + orderId);
    }
}
