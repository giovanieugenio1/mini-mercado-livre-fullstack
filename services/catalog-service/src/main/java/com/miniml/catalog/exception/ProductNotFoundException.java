package com.miniml.catalog.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID id) {
        super("Produto não encontrado: " + id);
    }
}
