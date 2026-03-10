# 📧 SMTP Configuration Guide - LeadFlow

Guia completo para configurar email em diferentes provedores.

## 🚀 Quick Start

### Opção Rápida: Gmail (Desenvolvimento)
1. Ativar [2-Step Verification](https://myaccount.google.com/security)
2. Gerar [App Password](https://myaccount.google.com/apppasswords)
3. Adicionar a `.env`:
   ```bash
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=seu-email@gmail.com
   MAIL_PASSWORD=seu-app-password
   ```

---

## 1️⃣ Gmail SMTP (Recomendado para Desenvolvimento)

### Pré-requisitos
- Google Account
- 2-Step Verification habilitado
- Permissão para gerar App Passwords

### Instruções Passo a Passo

#### A. Ativar 2-Step Verification
1. Acessar [Google Account Security](https://myaccount.google.com/security)
2. Em "How you sign in to Google", clicar em `2-Step Verification`
3. Seguir as instruções
4. Confirmar com seu telefone

#### B. Gerar App Password
1. Acessar [Google App Passwords](https://myaccount.google.com/apppasswords)
   - (Aparece só se 2-Step Verification estiver ativado)
2. Selecionar:
   - App: `Mail`
   - Device: `Other (custom name)` → Digite "LeadFlow"
3. Clicar em `Generate`
4. Copiar senha gerada (16 caracteres)

#### C. Configurar .env
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx  # Senha de 16 caracteres gerada
MAIL_FROM=seu-email@gmail.com
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_AUTH=true
```

#### D. Testar Conexão
```bash
# No terminal, usar telnet (ou nc)
telnet smtp.gmail.com 587
```

**Resposta esperada:**
```
220 smtp.gmail.com ESMTP ready
```

### ✅ Vantagens
- Rápido de configurar
- Grátis para testes
- Confiável
- Bom para desenvolvimento

### ❌ Desvantagens
- Não adequado para produção
- Limite de ~500 emails/dia
- Taxa de entrega pode ser baixa
- Pode ser bloqueado por spam filtering

---

## 2️⃣ SendGrid SMTP (Recomendado para Produção)

### Pré-requisitos
- [Conta SendGrid](https://sendgrid.com) (grátis para até 100 emails/dia)
- Domínio verificado
- API Key

### Instruções Passo a Passo

#### A. Criar Conta SendGrid
1. Acessar [sendgrid.com](https://sendgrid.com)
2. Clicar em `Start Free`
3. Preencher formulário com dados da empresa
4. Confirmar email

#### B. Verificar Domínio (Importante!)
1. Login no [SendGrid Dashboard](https://app.sendgrid.com)
2. Ir para `Settings` → `Sender Authentication`
3. Clicar em `Authenticate Your Domain`
4. Selecionar seu domínio (ex: `leadflow.com`)
5. Adicionar registros DNS (CNAME):
   ```
   Host: t1._domainkey.leadflow.com
   Value: sendgrid.net
   TTL: 3600
   
   Host: t2._domainkey.leadflow.com
   Value: sendgrid.net
   TTL: 3600
   ```
6. Esperar 24-48h para propagação completa

#### C. Criar API Key
1. Dashboard → `Settings` → `API Keys`
2. Clicar em `Create API Key`
3. Nome: `LeadFlow-Backend`
4. Permissions: `Mail Send` ✓
5. Clicar em `Create & Copy`
6. **SALVAR NUM LUGAR SEGURO** (não mostrar em logs!)

#### D. Configurar .env
```bash
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=SG.YOUR_API_KEY_HERE
MAIL_FROM=noreply@seu-dominio.com
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_AUTH=true
```

#### E. Testar Envio
```bash
# Via API (curl)
curl -X POST https://api.sendgrid.com/v3/mail/send \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer SG.YOUR_API_KEY' \
  -d '{
    "personalizations": [{
      "to": [{"email": "seu-email@example.com"}]
    }],
    "from": {"email": "noreply@seu-dominio.com"},
    "subject": "Test Email",
    "content": [{"type": "text/html", "value": "Test"}]
  }'
```

### ✅ Vantagens
- Excelente taxa de entrega
- Suporte dedicado
- Webhooks para rastreamento
- Plano grátis (100/dia)
- Ideal para produção

### ❌ Desvantagens
- Requer verificação de domínio (24-48h)
- Plano pago para volume maior
- Configuração mais complexa

### 📊 Preços SendGrid
- **Grátis:** 100 emails/dia
- **Pro:** $29.95/mês (até 100k emails) + $0.001 por email adicional
- **Enterprise:** Preço customizado

---

## 3️⃣ AWS SES SMTP (Recomendado para Alta Escala)

### Pré-requisitos
- [Conta AWS](https://aws.amazon.com)
- SES habilitado
- Domínio/Email verificado
- IAM User com SES permissions

### Instruções Passo a Passo

#### A. Ativar AWS SES
1. AWS Console → `SES` (Simple Email Service)
2. Verificar que está na região correta (ex: us-east-1)
3. Se necessário, fazer upgrade do "Sandbox Mode"

#### B. Verificar Domínio
1. SES Console → `Verified identities`
2. Clicar em `Create identity`
3. Selecionar `Domain`
4. Digitar seu domínio: `leadflow.com`
5. Adicionar registros DNS (TXT, DKIM):
   ```
   TXT: 
   Name: leadflow.com
   Value: v=spf1 include:amazonses.example.com ~all
   
   DKIM CNAME (3 records):
   Name: token1._domainkey.leadflow.com
   Value: token1.dkim.amazonses.com
   
   Name: token2._domainkey.leadflow.com
   Value: token2.dkim.amazonses.com
   
   Name: token3._domainkey.leadflow.com
   Value: token3.dkim.amazonses.com
   ```
6. Esperar verificação

#### C. Criar Credenciais SMTP
1. SES Console → `Account dashboard`
2. Clicarem `SMTP Settings`
3. Clicar em `Create SMTP credentials`
4. IAM user name: `leadflow-ses-user`
5. Clicar em `Create`
6. Download `credentials.csv` (NÃO perder!)

Arquivo conterá:
```
User Name: leadflow-ses-user
SMTP Username: AKIA7Z7XXXXX
SMTP Password: sWfH7jD.......
```

#### D. Configurar .env
```bash
# Exemplo: us-east-1 region
MAIL_HOST=email-smtp.us-east-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=AKIA7Z7XXXXX
MAIL_PASSWORD=sWfH7jD8V0cB3XyZ...
MAIL_FROM=noreply@seu-dominio.com
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_AUTH=true
```

#### E. Remover Sandbox Mode (Opcional)
No console SES:
1. `Account dashboard`
2. `Request production access`
3. Preencher formulário
4. AWS revisa em ~1 dia
5. Approval permite enviar para qualquer email

### ✅ Vantagens
- Very affordable ($1.00 per 10,000 requests)
- Sem limite de volume na produção
- Integração com AWS (se já usa)
- Excelente reliability

### ❌ Desvantagens
- Começa em Sandbox Mode (restrições)
- Requer conta AWS
- Configuração mais técnica
- Múltiplas regiões = múltiplas setup

### 💰 Preços AWS SES
- **Envio:** $0.10 por 1000 emails
- **Recepção:** $0.10 por 1000 emails
- Primeiros 62,000 emails/mês GRÁTIS (apenas envio)

---

## ⚙️ Configuração no Java

### Application Properties
```properties
# src/main/resources/application-prod.yml
spring:
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    default-encoding: UTF-8
    properties:
      mail:
        smtp:
          auth: ${MAIL_SMTP_AUTH:true}
          starttls:
            enable: ${MAIL_SMTP_STARTTLS_ENABLE:true}
            required: ${MAIL_SMTP_STARTTLS_REQUIRED:true}
          connectiontimeout: ${MAIL_SMTP_CONNECTIONTIMEOUT:5000}
          timeout: ${MAIL_SMTP_TIMEOUT:5000}
          writetimeout: ${MAIL_SMTP_WRITETIMEOUT:5000}
```

### Configuração Java
```java
@Configuration
public class MailConfig {
    
    @Bean
    public JavaMailSender mailSender(
        @Value("${spring.mail.host}") String host,
        @Value("${spring.mail.port}") int port,
        @Value("${spring.mail.username}") String username,
        @Value("${spring.mail.password}") String password) {
        
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        
        return sender;
    }
}
```

---

## 🧪 Testes de Email

### Teste Local (MailDev)
```bash
# 1. Instalar MailDev
npm install -g maildev

# 2. Iniciar servidor de testes
maildev

# 3. Configurar .env para apontar ao MailDev
MAIL_HOST=localhost
MAIL_PORT=1025

# 4. Acessar interface em: http://localhost:1080
```

### Teste com SendGrid Sandbox
```bash
# Gerar API key com acesso restrito
# SendGrid Dashboard → Settings → API Keys
# Permissions: "Mail Send" apenas (sem acesso a dados sensíveis)
```

### Logging de Emails
```bash
LOGGING_LEVEL_SPRING_MAIL=DEBUG
LOGGING_LEVEL_COM_LEADFLOW_EMAIL=DEBUG
```

---

## 🔒 Segurança

### ✅ Melhores Práticas

1. **Nunca commitar credenciais:**
   ```bash
   echo ".env" >> .gitignore
   echo ".env.local" >> .gitignore
   ```

2. **Usar secrets em produção:**
   ```bash
   # Docker/Kubernetes
   docker run -e MAIL_PASSWORD="$(aws secretsmanager get-secret-value ...)" ...
   ```

3. **Rotacionar API Keys regularmente:**
   - Trocar password/key a cada 3-6 meses
   - Revisar logs de acesso

4. **Monitorar deliverability:**
   ```bash
   # SendGrid metrics
   curl -X GET https://api.sendgrid.com/v3/stats \
     -H 'Authorization: Bearer SG.YOUR_KEY'
   ```

5. **SPF/DKIM/DMARC Records:**
   ```dns
   # SPF
   v=spf1 include:sendgrid.net ~all
   
   # DKIM (auto-configurado por provedor)
   # DMARC (opcional, mais seguro)
   v=DMARC1; p=quarantine; rua=mailto:dmarc-report@leadflow.com
   ```

---

## 📊 Comparação de Provedores

| Critério | Gmail | SendGrid | AWS SES |
|----------|-------|----------|---------|
| **Configuração** | ⭐⭐⭐ Fácil | ⭐⭐ Médio | ⭐ Complexo |
| **Custo** | Grátis | $29/mês | $0.10/1000 |
| **Volume** | 500/dia | 100k+/mês | Ilimitado |
| **Deliverability** | 80% | 98%+ | 99%+ |
| **Para Dev** | ✅ Ideal | ⚠️ Setup | ❌ Overkill |
| **Para Prod** | ❌ Não | ✅ Recomendado | ✅ Melhor |
| **Suporte** | ❌ Nenhum | ✅ 24/7 | ✅ 24/7 |

---

## 🚨 Troubleshooting

### "Authentication failed"
```
❌ Erro: 535 5.7.8 Authentication credentials invalid
✅ Solução:
  - Gmail: Verificar App Password (16 chars)
  - SendGrid: Validar API Key
  - AWS SES: Verificar IAM credentials
```

### "STARTTLS required"
```
❌ Erro: 530 5.7.0 Must issue a STARTTLS command first
✅ Solução:
  - Definir MAIL_SMTP_STARTTLS_ENABLE=true
  - Usar MAIL_PORT 587 (não 25 ou 465)
```

### "Connection timeout"
```
❌ Erro: Connection timeout to smtp.xxx.com:587
✅ Solução:
  - Verificar firewall (liberar porta 587)
  - Testar: telnet smtp.xxx.com 587
  - Aumentar timeout em properties
```

### "Emails não chegam"
```
❌ Problema: Email enviado mas não recebido
✅ Solução:
  - Verificar SPF/DKIM records
  - Testar email em https://mxtoolbox.com
  - Verificar spam folder
  - Revisar logs do provedor
```

---

_Última atualização: Março 2026_
