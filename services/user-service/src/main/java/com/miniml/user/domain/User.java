package com.miniml.user.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(unique = true, nullable = false, length = 14)
    private String cpf;

    @Embedded
    private AddressData address;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected User() {}

    public User(String firstName, String lastName, String email, String cpf, AddressData address) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.email     = email;
        this.cpf       = cpf;
        this.address   = address;
    }

    public void assignKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
    }

    public UUID          getId()          { return id; }
    public String        getKeycloakId()  { return keycloakId; }
    public String        getFirstName()   { return firstName; }
    public String        getLastName()    { return lastName; }
    public String        getEmail()       { return email; }
    public String        getCpf()         { return cpf; }
    public AddressData   getAddress()     { return address; }
    public boolean       isActive()       { return active; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
}
