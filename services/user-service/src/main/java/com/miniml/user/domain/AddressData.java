package com.miniml.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AddressData {

    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    @Column(length = 2)
    private String estado;
    private String cep;

    protected AddressData() {}

    public AddressData(String logradouro, String numero, String complemento,
                       String bairro, String cidade, String estado, String cep) {
        this.logradouro  = logradouro;
        this.numero      = numero;
        this.complemento = complemento;
        this.bairro      = bairro;
        this.cidade      = cidade;
        this.estado      = estado;
        this.cep         = cep;
    }

    public String getLogradouro()  { return logradouro; }
    public String getNumero()      { return numero; }
    public String getComplemento() { return complemento; }
    public String getBairro()      { return bairro; }
    public String getCidade()      { return cidade; }
    public String getEstado()      { return estado; }
    public String getCep()         { return cep; }
}
