package com.miniml.user.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false, length = 255)
    private String username;  

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash; 

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected UserCredential() {}

    public UserCredential(User user, String username, String passwordHash) {
        this.user         = user;
        this.username     = username;
        this.passwordHash = passwordHash;
    }

    public UUID          getId()           { return id; }
    public User          getUser()         { return user; }
    public String        getUsername()     { return username; }
    public String        getPasswordHash() { return passwordHash; }
    public boolean       isActive()        { return active; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
}
