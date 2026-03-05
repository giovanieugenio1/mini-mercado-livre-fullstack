# Mini Mercado Livre — Fullstack Microservices

Plataforma de e-commerce construída com arquitetura de microsserviços, demonstrando padrões avançados de sistemas distribuídos: Saga via eventos Kafka, Outbox Pattern, idempotência, cache Redis, observabilidade com OpenTelemetry e autenticação OAuth2/OIDC com Keycloak.

---

## Arquitetura

```
                          ┌─────────────────────────────────┐
  Browser / Mobile        │          API Gateway             │  :8079
  ─────────────────  ───► │  Spring Cloud Gateway + JWT Auth │
                          └──────────────┬──────────────────┘
                                         │  roteia para
              ┌──────────────────────────┼──────────────────────────┐
              │                          │                           │
        :8081 ▼              :8082 ▼     │  :8083 ▼      :8084 ▼    │
   ┌──────────────┐   ┌──────────────┐  │ ┌──────────┐ ┌─────────┐ │
   │   Catalog    │   │    Order     │  │ │ Payment  │ │Inventory│ │
   │   Service    │   │   Service    │  │ │ Service  │ │ Service │ │
   └──────┬───────┘   └──────┬───────┘  │ └────┬─────┘ └────┬────┘ │
          │                  │           │      │             │      │
          │           ┌──────▼───────────▼──────▼─────────────▼──┐  │
          │           │              Apache Kafka                  │  │
          │           │  order.created  payment.authorized         │  │
          │           │  inventory.reserved  shipping.created      │  │
          │           └───────────────────┬──────────────────────┘  │
          │                               │ consome                  │
          │                     ┌─────────┴──────────┐              │
          │                :8085 ▼               :8086 ▼            │
          │           ┌──────────────┐   ┌──────────────────┐       │
          │           │   Shipping   │   │  Notification    │       │
          │           │   Service    │   │    Service       │       │
          │           └──────────────┘   └──────────────────┘       │
          │                                                          │
          │       :8087                                              │
          │   ┌──────────────┐                                       │
          └──►│ User Service │  (cadastro + Keycloak Admin API)      │
              └──────────────┘                                       │
                                                                     │
  ┌──────────────────────────────────────────────────────────────┐   │
  │  Infraestrutura                                              │   │
  │  Keycloak :8080 │ Redis :6379 │ PostgreSQL :5432–5438       │   │
  │  Prometheus :9090 │ Grafana :3001 │ Jaeger :16686           │   │
  │  Kafka UI :8090                                             │   │
  └──────────────────────────────────────────────────────────────┘   │
```

### Fluxo de compra (Saga Coreografada)

```
Cliente faz pedido
    └─► order-service  ──── order.created.v1 ──────────────────────┐
                                                                    │
    payment-service  ◄── consome ── autoriza pagamento              │
        └─► payment.authorized.v1 ──────────────────────────────┐  │
                                                                 │  │
    inventory-service ◄── consome ── reserva estoque            │  │
        └─► inventory.reserved.v1 ─────────────────────────┐   │  │
                                                            │   │  │
    shipping-service ◄── consome ── cria envio              │   │  │
        └─► shipping.created.v1 ──────────────────────┐    │   │  │
                                                       │    │   │  │
    notification-service ◄── consome todos os eventos  │    │   │  │
        └─► envia e-mail / notificação ao cliente      │    │   │  │
```

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 17, Spring Boot 3.4, Spring Cloud Gateway 2024 |
| Mensageria | Apache Kafka 3.9.2 (KRaft, sem ZooKeeper) |
| Banco de dados | PostgreSQL 15 (um por serviço) |
| Cache | Redis 7 |
| Autenticação | Keycloak 24 (OAuth2/OIDC, PKCE) |
| Migrações | Flyway 10 |
| Observabilidade | OpenTelemetry, Jaeger, Prometheus, Grafana |
| Frontend | Angular 19 (standalone, signals) |
| Containerização | Docker, Docker Compose |
| Documentação API | OpenAPI 3 / Swagger UI agregado |

---

## Pré-requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 4.x
- Docker Compose v2
- 8 GB RAM disponíveis (recomendado)
- Portas livres: 4200, 8079–8090, 9090, 9092, 9094, 3001, 6379, 16686, 4317–4318

---

## Início Rápido

### Opção 1 — Build local (recomendado para desenvolvimento)

```bash
# 1. Clonar o repositório
git clone https://github.com/seu-usuario/mini-mercado-livre-fullstack.git
cd mini-mercado-livre-fullstack

# 2. Subir toda a stack (infra + serviços + frontend)
docker-compose up --build

# Aguarde ~3 minutos enquanto os serviços sobem na ordem correta
# Keycloak demora ~90s para ficar pronto
```

### Opção 2 — Imagens do Docker Hub

```bash
# Substituir SEU_USUARIO pelo seu Docker Hub username
REGISTRY=SEU_USUARIO docker-compose up
```

### Opção 3 — Apenas infraestrutura (para desenvolvimento local dos serviços)

```bash
# Sobe apenas: PostgreSQL, Kafka, Redis, Keycloak, Jaeger, Prometheus, Grafana
cd infra
docker-compose up
```

---

## Acessos após subir

| Serviço | URL | Credenciais |
|---|---|---|
| Frontend | http://localhost:4200 | user1 / user1pass |
| API Gateway | http://localhost:8079 | JWT Bearer |
| Swagger UI Agregado | http://localhost:8079/swagger-ui.html | — |
| Keycloak Admin | http://localhost:8080 | admin / admin |
| Kafka UI | http://localhost:8090 | — |
| Grafana | http://localhost:3001 | admin / admin |
| Jaeger Tracing | http://localhost:16686 | — |
| Prometheus | http://localhost:9090 | — |

---

## Desenvolvimento Local

Para rodar os serviços diretamente na JVM (sem Docker), suba apenas a infra e depois cada serviço individualmente:

```bash
# 1. Infra
cd infra && docker-compose up -d

# 2. Cada serviço (em terminais separados)
cd services/catalog-service    && mvn spring-boot:run
cd services/order-service      && mvn spring-boot:run
cd services/payment-service    && mvn spring-boot:run
cd services/inventory-service  && mvn spring-boot:run
cd services/shipping-service   && mvn spring-boot:run
cd services/notification-service && mvn spring-boot:run
cd services/user-service       && mvn spring-boot:run
cd services/api-gateway        && mvn spring-boot:run

# 3. Frontend
cd frontend/angular-app && npm install && npm start
```

---

## Portas dos Serviços

| Serviço | Porta |
|---|---|
| Frontend (Angular) | 4200 |
| API Gateway | 8079 |
| Keycloak | 8080 |
| catalog-service | 8081 |
| order-service | 8082 |
| payment-service | 8083 |
| inventory-service | 8084 |
| shipping-service | 8085 |
| notification-service | 8086 |
| user-service | 8087 |
| Kafka UI | 8090 |
| Kafka (externo) | 9094 |
| Redis | 6379 |
| Prometheus | 9090 |
| Grafana | 3001 |
| Jaeger UI | 16686 |

---

## Usuários de Teste

| Usuário | Senha | Role |
|---|---|---|
| user1 | user1pass | ROLE_USER |
| admin1 | admin1pass | ROLE_USER, ROLE_ADMIN |

---

## Endpoints Principais

### Produtos (público para leitura)
```
GET  /api/v1/products?page=0&size=12&query=celular&category=eletronicos
GET  /api/v1/products/{id}
POST /api/v1/products          (ROLE_ADMIN)
PUT  /api/v1/products/{id}     (ROLE_ADMIN)
DELETE /api/v1/products/{id}   (ROLE_ADMIN)
```

### Pedidos (autenticado)
```
POST /api/v1/orders            { customerId, items: [{productId, productTitle, unitPrice, quantity}] }
GET  /api/v1/orders/{id}
GET  /api/v1/orders/customer/{customerId}
```

### Estoque (ROLE_ADMIN)
```
GET  /api/v1/inventory/{productId}
PUT  /api/v1/inventory/{productId}
```

### Usuários (público)
```
POST /users/register           { firstName, lastName, email, cpf, password, address? }
```

---

## Tópicos Kafka

| Tópico | Produtor | Consumidores |
|---|---|---|
| `order.created.v1` | order-service | payment-service |
| `payment.authorized.v1` | payment-service | inventory-service |
| `payment.failed.v1` | payment-service | notification-service |
| `inventory.reserved.v1` | inventory-service | shipping-service |
| `inventory.failed.v1` | inventory-service | notification-service |
| `shipping.created.v1` | shipping-service | notification-service |
| `shipping.failed.v1` | shipping-service | notification-service |
| `*.DLT` | — | Dead Letter Topics |

---

## Padrões Implementados

- **Outbox Pattern** — evento Kafka gravado na mesma transação do domínio (`outbox_event`)
- **Idempotência** — tabela `processed_event` por consumidor evita processamento duplicado
- **Saga Coreografada** — fluxo de compra sem orquestrador central
- **Database per Service** — cada microsserviço tem seu próprio banco
- **API Gateway** — ponto único de entrada com autenticação JWT
- **Cache** — Redis com TTL 30min para consulta de produtos
- **Observabilidade** — traces distribuídos (OTLP → Jaeger), métricas (Prometheus + Grafana)
- **RFC 7807** — respostas de erro padronizadas (ProblemDetail)

---

## Testes

```bash
# Testes unitários e de controller (sem Docker)
cd services/user-service
mvn test -Dtest="CpfValidatorTest,UserControllerTest,UserRegistrationServiceTest"

# Todos os testes de um serviço (requer Docker para Testcontainers)
cd services/catalog-service
mvn test
```

---

## Docker Hub — Build e Push

```bash
# Definir seu usuário
export REGISTRY=seu-dockerhub-username

# Build de todas as imagens
docker-compose build

# Login no Docker Hub
docker login

# Push de todas as imagens
docker-compose push

# Ou individualmente
docker push $REGISTRY/catalog-service:latest
docker push $REGISTRY/order-service:latest
docker push $REGISTRY/payment-service:latest
docker push $REGISTRY/inventory-service:latest
docker push $REGISTRY/shipping-service:latest
docker push $REGISTRY/notification-service:latest
docker push $REGISTRY/user-service:latest
docker push $REGISTRY/api-gateway:latest
docker push $REGISTRY/frontend:latest
```

---

## Estrutura do Projeto

```
mini-mercado-livre-fullstack/
├── docker-compose.yml              # Stack completa (infra + serviços + frontend)
├── infra/
│   ├── docker-compose.yml          # Apenas infraestrutura
│   ├── keycloak/realm-export.json  # Realm pré-configurado
│   ├── kafka/init-topics.sh        # Criação de tópicos
│   ├── prometheus/prometheus.yml
│   └── grafana/provisioning/
├── services/
│   ├── catalog-service/            # Catálogo de produtos + cache Redis
│   ├── order-service/              # Gestão de pedidos
│   ├── payment-service/            # Processamento de pagamentos
│   ├── inventory-service/          # Controle de estoque
│   ├── shipping-service/           # Gestão de entregas
│   ├── notification-service/       # Notificações por e-mail/evento
│   ├── user-service/               # Cadastro de usuários + Keycloak
│   └── api-gateway/                # Gateway + roteamento + JWT
└── frontend/
    └── angular-app/                # SPA Angular 19
```

---

## Licença

MIT
