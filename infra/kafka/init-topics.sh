#!/bin/bash
# =============================================================
# Mini Mercado Livre — Criação de tópicos Kafka
# Executado pelo container kafka-init após kafka estar healthy
# =============================================================

set -e

BOOTSTRAP="kafka:9092"
PARTITIONS=3
REPLICATION=1

# apache/kafka scripts ficam em /opt/kafka/bin/
KAFKA_TOPICS="/opt/kafka/bin/kafka-topics.sh"

echo "⏳ Aguardando Kafka ficar disponível em $BOOTSTRAP..."
$KAFKA_TOPICS --bootstrap-server "$BOOTSTRAP" --list > /dev/null 2>&1
echo "✅ Kafka disponível!"

# Função auxiliar
create_topic() {
  local topic=$1
  echo "   → Criando tópico: $topic"
  $KAFKA_TOPICS \
    --bootstrap-server "$BOOTSTRAP" \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions "$PARTITIONS" \
    --replication-factor "$REPLICATION"
}

echo ""
echo "📦 Criando tópicos de domínio..."

# Saga de pedidos
create_topic "order.created.v1"
create_topic "order.completed.v1"

# Saga de pagamento
create_topic "payment.authorized.v1"
create_topic "payment.failed.v1"

# Saga de estoque
create_topic "inventory.reserved.v1"
create_topic "inventory.failed.v1"

# Saga de envio
create_topic "shipping.created.v1"
create_topic "shipping.failed.v1"

# Dead Letter Topics (DLT) — reprocessamento manual
create_topic "order.created.v1.DLT"
create_topic "payment.authorized.v1.DLT"
create_topic "inventory.reserved.v1.DLT"

echo ""
echo "📋 Tópicos existentes:"
$KAFKA_TOPICS --bootstrap-server "$BOOTSTRAP" --list

echo ""
echo "🎉 Todos os tópicos criados com sucesso!"
