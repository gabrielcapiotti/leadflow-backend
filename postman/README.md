# 📬 Postman Collection - LeadFlow Billing API

Guia completo para usar a Postman Collection de Billing do LeadFlow.

## 📥 Importar Collection

### Opção 1: Via Arquivo JSON
1. Abrir Postman
2. `File` → `Import`
3. Selecionar arquivo `LeadFlow-Billing-API.postman_collection.json`
4. Clicar em `Import`

### Opção 2: Via URL (não disponível ainda)
```
https://api.leadflow.com/postman/collection
```

---

## ⚙️ Configurar Environment

A collection utiliza variáveis de ambiente. Configure assim:

### 1. Criar Environment
```
File → Settings → + (New Environment)
```

### 2. Definir Variáveis

| Variável | Valor | Descrição |
|----------|-------|-----------|
| `base_url` | `http://localhost:8080` | URL base da API |
| `jwt_token` | `seu_token_jwt_aqui` | Token JWT válido |
| `customer_email` | `usuario@example.com` | Email do cliente |
| `tenant_id` | `550e8400-e29b-41d4-a716-446655440000` | ID do tenant |
| `stripe_webhook_secret` | `whsec_test_...` | Stripe webhook secret |

### 3. Usar Environment
```
No canto superior direito → Dropdown → Selecionar seu environment
```

---

## 🧪 Executar Requisições

###1️⃣ **Criar Checkout**

**Folder:** `1. Checkout`  
**Request:** `Create Checkout Session`

**O que faz:**
- Cria uma sessão de checkout Stripe
- Retorna URL de checkout

**Requisitos:**
- ✅ `jwt_token` configurado
- ✅ `customer_email` válido

**Passo a passo:**
1. Ir para `1. Checkout` → `Create Checkout Session`
2. Clicar em `Send`
3. Verificar resposta (deve retornar `checkoutUrl`)

**Exemplo de Resposta:**
```json
{
  "checkoutUrl": "https://checkout.stripe.com/pay/cs_test_1234567890",
  "sessionId": "cs_test_1234567890",
  "provider": "stripe",
  "expiresAt": "2026-03-10T12:30:00Z"
}
```

---

### 2️⃣ **Webhook - Subscription Created**

**Folder:** `2. Webhooks`  
**Request:** `Webhook - Subscription Created`

**O que faz:**
- Simula webhook de subscription criada
- Valida assinatura HMAC-SHA256
- Processa evento no servidor

**Script Pré-Requisição:**
- ✅ Gera timestamp válido
- ✅ Computa assinatura correta
- ✅ Valida certificado

**Passo a passo:**
1. Ir para `2. Webhooks` → `Webhook - Subscription Created`
2. Clicar em `Send`
3. Esperarisponse `200 OK - Webhook processed`

**O que acontece no servidor:**
- ✅ Email de confirmação enviado
- ✅ Métricas registradas
- ✅ Evento processado no banco

---

### 3️⃣ **Webhook - Payment Failed**

**Folder:** `2. Webhooks`  
**Request:** `Webhook - Payment Failed`

**O que faz:**
- Simula webhook de pagamento falhado
- Aciona email de notificação
- Registra tentativa de pagamento

**Passo a passo:**
1. Ir para `2. Webhooks` → `Webhook - Payment Failed`
2. Clicar em `Send`
3. Verificar resposta `200 OK`

**Efeitos esperados:**
- 📧 Email enviado para customer
- 📊 Métrica de falha registrada
- 🔔 Alerta gerado (se threshold atingido)

---

### 4️⃣ **Testes de Erro**

#### Webhook com Signature Inválida
```
2. Webhooks → Webhook - Invalid Signature (Teste)
```

**Esperado:** `400 Bad Request - Invalid webhook signature`

#### Webhook sem Header de Signature
```
2. Webhooks → Webhook - Missing Signature Header (Teste)
```

**Esperado:** `400 Bad Request - Missing Stripe-Signature header`

---

### 5️⃣ **Monitorar Métricas**

**Folder:** `3. Monitoring`  
**Request:** `Prometheus Metrics`

**O que faz:**
- Retorna todas as métricas de webhook
- Formato Prometheus
- Compatível com Grafana

**URL:** `GET /actuator/prometheus`

**Métricas esperadas:**
```
webhook_processing_duration_seconds
webhook_processing_success_total
webhook_processing_failure_total
webhook_signature_validation_total
webhook_timestamp_validation_total
```

---

## 🔄 Fluxo de Teste Completo

### Cenário: Pagamento de Assinatura

1. **POST /checkout** - Criar sessão
   - Usuário clica em "Pagar"
   - Recebe URL de checkout

2. **Usuário vai para Stripe** - Preenche dados de cartão
   - Clica em "Pay"
   - Stripe processa pagamento

3. **Webhook - Subscription Created** - Simulator
   - Stripe envia evento
   - Sistema recebe e processa
   - Email de confirmação enviado

4. **Verificar Métricas** - Monitorar
   - Taxa de sucesso = 100%
   - Latência P95 = X ms
   - Eventos processados = 1

---

## 🔐 Segurança em Testes

### ⚠️ IMPORTANTE: Assinatura de Webhook

A collection usa script de **pré-requisição** para gerar assinatura válida:

```javascript
const crypto = require('crypto');
const secret = pm.environment.get('stripe_webhook_secret');
const timestamp = Math.floor(Date.now() / 1000).toString();

const payload = JSON.stringify({ ... });
const signedContent = timestamp + '.' + payload;
const signature = crypto
  .createHmac('sha256', secret)
  .update(signedContent)
  .digest('hex');
```

**Validação:**
- ✅ Timestamp recente (< 5 min)
- ✅ Signature matches webhook secret
- ✅ Payload não foi alterado

---

## 🐛 Troubleshooting

### Erro: "JWT token not found"
```
❌ Error: JWT token não está definido
✅ Solução:
  1. Environment → jwt_token
  2. Copiar token válido
  3. Salvar
  4. Tentar novamente
```

### Erro: "Invalid webhook signature"
```
❌ Error: Invalid webhook signature
✅ Solução:
  1. Verificar stripe_webhook_secret está correto
  2. Tentar novamente (script gera novo timestamp)
  3. Checar logs do servidor
```

### Erro: "Connection refused"
```
❌ Error: Cannot connect to localhost:8080
✅ Solução:
  1. Verificar se servidor está rodando
  2. mvn spring-boot:run
  3. Esperar início (leva ~10s)
  4. Tentar novamente
```

### Erro: "401 Unauthorized"
```
❌ Error: 401 Unauthorized
✅ Solução:
  1. JWT token pode estar expirado
  2. Gerar novo token
  3. Atualizar em jwt_token
  4. Tentar novamente
```

---

## 📊 Validação Automática

Todos os requests têm **testes automáticos** que verificam:

### Response da Checkout
- ✅ Status 200
- ✅ Contém `checkoutUrl`
- ✅ `provider` = "stripe"
- ✅ SessionId salvo para referência

### Webhooks
- ✅ Status 200
- ✅ Mensagem "Webhook processed"
- ✅ Content-Type correto

### Testes de Erro
- ✅ Status 400 (para signature inválida)
- ✅ Mensagem de erro apropriada

**Ver resultados:** Aba `Tests` após enviar request

---

## 🚀 Boas Práticas

### ✅ DO:
- Sempre configurar environment antes de usar
- Rodar testes de erro para verificar validação
- Monitorar métricas após cada teste
- Manter webhook secret seguro

###❌ DON'T:
- ❌ Usar `base_url` hardcoded
- ❌ Compartilhar jwt_token ou webhook_secret
- ❌ Alterar payloads de webhook sem razão
- ❌ Ignorar erros de validação

---

## 📞 Suporte

Encontrou problema?

1. Verificar variáveis de environment
2. Revisar logs do servidor
3. Tentar novamente
4. Contatar: api-support@leadflow.com

---

## 📚 Recursos Adicionais

- [API Spec Completa](/docs/API_BILLING_SPEC.md)
- [Stripe Docs](https://stripe.com/docs)
- [Postman Learning Center](https://learning.postman.com)
- [OpenAPI Spec](/swagger-ui.html)

---

_Última atualização: Março 2026_
