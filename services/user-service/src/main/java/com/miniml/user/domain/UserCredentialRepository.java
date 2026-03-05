package com.miniml.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
}
