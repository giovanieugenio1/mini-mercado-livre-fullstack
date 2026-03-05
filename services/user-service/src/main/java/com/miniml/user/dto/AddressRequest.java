package com.miniml.user.dto;

public record AddressRequest(
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        String cep
) {}
