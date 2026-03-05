-- Tabela principal de usuários
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id     VARCHAR(36) UNIQUE,
    first_name      VARCHAR(100)        NOT NULL,
    last_name       VARCHAR(100)        NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    cpf             VARCHAR(14)  UNIQUE NOT NULL,

    -- Endereço (opcional)
    logradouro      VARCHAR(255),
    numero          VARCHAR(20),
    complemento     VARCHAR(100),
    bairro          VARCHAR(100),
    cidade          VARCHAR(100),
    estado          CHAR(2),
    cep             VARCHAR(9),

    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Tabela de credenciais (senha criptografada com BCrypt)
CREATE TABLE user_credentials (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    username        VARCHAR(255) UNIQUE NOT NULL,   -- igual ao e-mail
    password_hash   VARCHAR(255)        NOT NULL,   -- BCrypt ($2a$...)
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_credentials_user_id ON user_credentials(user_id);
CREATE INDEX idx_users_email              ON users(email);
CREATE INDEX idx_users_cpf               ON users(cpf);
