# 📋 Especificação da API de Billing - LeadFlow

**Versão:** 1.0.0  
**Data:** Março 2026  
**Status:** Production Ready  

---

## 📑 Sumário

1. [Overview](#overview)
2. [Autenticação](#autenticação)
3. [Endpoints](#endpoints)
4. [Webhooks](#webhooks)
5. [Métricas e Monitoring](#métricas-e-monitoring)
6. [Tratamento de Erros](#tratamento-de-erros)
7. [Exemplos de Requisição](#exemplos-de-requisição)

---

## Overview

A API de Billing do LeadFlow integra **Stripe** para processamento de pagamentos seguro e confiável. Todos os endpoints de webhook validam a integridade das mensagens usando **HMAC-SHA256** e **timestamp validation** para prevenir ataques de replay.

### Base URL
```
https://api.leadflow.com/billing
```

### Headers Padrão
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>  (exceto webhooks)
```

---

## Autenticação

### Checkout (POST /checkout)
Requer autenticação JWT válida.

**Token JWT no header:**
```bash
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Webhooks (POST /webhook)
**Não requer JWT.** Validação via Stripe-Signature header (HMAC-SHA256).

---

## Endpoints

### 1. POST /checkout - Criar Sessão de Checkout

**Descrição:** Inicia uma nova sessão de checkout Stripe para pagamento de assinatura.

**URL:** `POST /billing/checkout`

**Headers Obrigatórios:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "usuario@example.com",
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "planId": "plan_standard"
}
```

**Campos:**
| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-----------|-----------|
| email | string | ✅ Sim | Email do cliente (formato válido) |
| tenantId | uuid | ❌ Não | ID do tenant para multi-tenant |
| planId | string | ❌ Não | ID do plano (padrão: 'plan_standard') |

**Response Success (HTTP 200):**
```json
{
  "checkoutUrl": "https://checkout.stripe.com/pay/cs_test_1234567890abcdef",
  "sessionId": "cs_test_1234567890abcdef",
  "provider": "stripe",
  "expiresAt": "2026-03-10T12:30:00Z"
}
```

**Response Error (HTTP 400):**
```json
{
  "error": "Invalid email format",
  "message": "Email must be a valid email address",
  "timestamp": "2026-03-10T10:30:00Z"
}
```

**Códigos de Resposta:**
| Código | Descrição |
|--------|-----------|
| 200 | Sessão criada com sucesso |
| 400 | Email inválido ou request malformado |
| 401 | Não autenticado |
| 500 | Erro na API Stripe ou servidor |

**Exemplo cURL:**
```bash
curl -X POST https://api.leadflow.com/billing/checkout \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@example.com",
    "tenantId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

---

### 2. POST /webhook - Receber Eventos Stripe

**Descrição:** Webhook para receber e processar eventos do Stripe (assinatura, pagamento, fatura).

**URL:** `POST /billing/webhook`

**Headers Obrigatórios:**
```
Content-Type: application/json
Stripe-Signature: t=1614556800,v1=5257a869e7ecebeda32affa62cdca3fa51cad7e77a0e56ff536d0ce8e108d8bd
```

**Stripe-Signature Format:**
```
t=<timestamp_unix>,v1=<hmac_sha256_signature>
```

**Request Body (Exemplo - subscription.created):**
```json
{
  "id": "evt_1234567890",
  "type": "customer.subscription.created",
  "created": 1614556800,
  "livemode": false,
  "data": {
    "object": {
      "id": "sub_1234567890",
      "customer": "cus_1234567890",
      "status": "active",
      "cancel_at_period_end": false,
      "current_period_start": 1614556800,
      "current_period_end": 1617235200,
      "metadata": {
        "tenant_id": "550e8400-e29b-41d4-a716-446655440000"
      }
    }
  }
}
```

**Eventos Suportados:**
| Evento | Descrição |
|--------|-----------|
| `customer.subscription.created` | Nova assinatura criada |
| `customer.subscription.updated` | Assinatura atualizada |
| `customer.subscription.deleted` | Assinatura cancelada |
| `invoice.payment_succeeded` | Pagamento de fatura bem-sucedido |
| `invoice.payment_failed` | Falha no pagamento de fatura |
| `charge.refunded` | Cobrança reembolsada |

**Response Success (HTTP 200):**
```
Webhook processed
```

**Response Error (HTTP 400):**
```
Invalid webhook signature
```
ou
```
Webhook timestamp too old
```
ou
```
Missing Stripe-Signature header
```

**Códigos de Resposta:**
| Código | Descrição |
|--------|-----------|
| 200 | Webhook processado com sucesso |
| 400 | Signature inválida, timestamp expirado, ou header faltando |
| 500 | Erro inesperado no processamento |

**Segurança:**
- ✅ Validação HMAC-SHA256 obrigatória
- ✅ Validação de timestamp (máximo 5 minutos de idade)
- ✅ Proteção contra replay attacks
- ✅ Rastreamento de eventos em métricas

**Exemplo Signature Validation (Node.js):**
```javascript
const crypto = require('crypto');

function verifyStripeSignature(payload, signature, secret) {
  const timestamp = signature.split(',')[0].split('=')[1];
  const sig = signature.split(',')[1].split('=')[1];
  
  const signedContent = `${timestamp}.${payload}`;
  const expectedSig = crypto
    .createHmac('sha256', secret)
    .update(signedContent)
    .digest('hex');
    
  return crypto.timingSafeEqual(sig, expectedSig);
}
```

---

## Webhooks

### Procesamento Garantido

Webhooks são processados de forma robusta com retry automático:

**Retry Strategy:**
- Tentativa 1: Imediata
- Tentativa 2: +5 minutos
- Tentativa 3: +1 hora
- Tentativa 4: +24 horas

**Status de Entrega:**
- ✅ Success: 200-299
- ⚠️ Retry: 500-599 ou timeout
- ❌ Failed: 400-499 (sem retry)

### Webhook Alerts

Quando múltiplas falhas são detectadas, alertas são enviados:

- **Threshold:** 5 falhas em 5 minutos
- **Cooldown:** 1 hora entre alertas
- **Canais:** Log + Email (configurável)

---

## Métricas e Monitoring

### Prometheus Endpoint

**URL:** `GET /actuator/prometheus`

**Métricas Disponíveis:**

#### Timer - Processamento
```
webhook_processing_duration_seconds{quantile="0.5"} 0.045
webhook_processing_duration_seconds{quantile="0.95"} 0.120
webhook_processing_duration_seconds{quantile="0.99"} 0.156
```

#### Counter - Sucesso
```
webhook_processing_success_total{event_type="customer.subscription.created"} 1024.0
webhook_processing_success_total{event_type="invoice.payment_succeeded"} 892.0
```

#### Counter - Falha
```
webhook_processing_failure_total{event_type="invoice.payment_failed",error_type="payment_declined"} 23.0
webhook_processing_failure_total{event_type="all",error_type="invalid_signature"} 5.0
```

#### Validation - Assinatura
```
webhook_signature_validation_total{result="valid"} 5000.0
webhook_signature_validation_total{result="invalid"} 12.0
```

#### Validation - Timestamp
```
webhook_timestamp_validation_total{result="valid"} 5000.0
webhook_timestamp_validation_total{result="expired"} 2.0
```

### Grafana Dashboard

Recomenda-se criar um dashboard com:
- Taxa de sucesso (%)
- Latência P50/P95/P99
- Contagem por tipo de evento
- Taxa de falhas por erro
- AlertasWebhook

---

## Tratamento de Erros

### Erros de Validação (400)

#### Signature Inválida
```json
{
  "error": "INVALID_SIGNATURE",
  "message": "Invalid webhook signature",
  "code": 400,
  "timestamp": "2026-03-10T10:30:00Z"
}
```

#### Timestamp Expirado
```json
{
  "error": "EXPIRED_TIMESTAMP",
  "message": "Webhook timestamp too old (máximo 5 minutos)",
  "code": 400,
  "timestamp": "2026-03-10T10:30:00Z"
}
```

#### Header Faltando
```json
{
  "error": "MISSING_SIGNATURE_HEADER",
  "message": "Missing Stripe-Signature header",
  "code": 400,
  "timestamp": "2026-03-10T10:30:00Z"
}
```

### Erros de Servidor (500)

```json
{
  "error": "INTERNAL_SERVER_ERROR",
  "message": "Erro ao processar webhook: Database connection failed",
  "code": 500,
  "timestamp": "2026-03-10T10:30:00Z",
  "traceId": "abc-123-def"
}
```

---

## Exemplos de Requisição

### JavaScript/Node.js

```javascript
// Criar checkout
const response = await fetch('https://api.leadflow.com/billing/checkout', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'usuario@example.com',
    tenantId: '550e8400-e29b-41d4-a716-446655440000'
  })
});

const { checkoutUrl } = await response.json();
window.location.href = checkoutUrl;
```

### Python

```python
import requests
import json

headers = {
    'Authorization': f'Bearer {token}',
    'Content-Type': 'application/json'
}

payload = {
    'email': 'usuario@example.com',
    'tenantId': '550e8400-e29b-41d4-a716-446655440000'
}

response = requests.post(
    'https://api.leadflow.com/billing/checkout',
    headers=headers,
    json=payload
)

checkout_url = response.json()['checkoutUrl']
```

### cURL

```bash
# Criar Checkout
curl -X POST https://api.leadflow.com/billing/checkout \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@example.com",
    "tenantId": "550e8400-e29b-41d4-a716-446655440000"
  }'

# Simular Webhook (para testes)
curl -X POST https://api.leadflow.com/billing/webhook \
  -H "Stripe-Signature: t=1614556800,v1=signature_hash" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "customer.subscription.created",
    "data": { "object": { "id": "sub_test" } }
  }'
```

---

## Variáveis de Ambiente

```bash
# Stripe
STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_test_...

# Email (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=sua-senha-app
MAIL_FROM=billing@leadflow.com
MAIL_REPLY_TO=support@leadflow.com

# Frontend URL (para links em emails)
FRONTEND_URL=https://app.leadflow.com

# Webhook Alert
STRIPE_WEBHOOK_ALERT_THRESHOLD=5
STRIPE_WEBHOOK_ALERT_EMAIL=ops@leadflow.com
```

---

## Status Codes

| Código | Significado |
|--------|-------------|
| 200 | Sucesso |
| 400 | Request inválido |
| 401 | Não autenticado |
| 403 | Não autorizado |
| 500 | Erro no servidor |
| 503 | Serviço indisponível |

---

## Contato & Suporte

Para dúvidas sobre a API:
- **Email:** api-support@leadflow.com
- **Status Page:** https://status.leadflow.com
- **Slack:** #billing-api

---

_Última atualização: Março 2026_
