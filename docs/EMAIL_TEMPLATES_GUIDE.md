# 📧 Email Templates Production Guide - LeadFlow

Complete guide to LeadFlow's production-ready email templates, customization, and best practices.

## 📋 Quick Start

### Available Templates
1. **subscription-confirmation.html** - Sent when user subscribes
2. **payment-failed.html** - Sent when payment is declined
3. **invoice-notification.html** - Sent after successful payment
4. **refund-notification.html** - Sent when refund is processed

### Sending an Email Template
```java
@Autowired
private EmailAlertService emailAlertService;

// Send subscription confirmation
emailAlertService.sendEmailTemplate(
    "email/subscription-confirmation",
    Map.of(
        "customerName", "John Doe",
        "planName", "Professional",
        "amount", "$99.00",
        "currency", "USD",
        "billingFrequency", "Monthly",
        "nextBillingDate", "April 10, 2026",
        "subscriptionId", "sub_1234567890",
        "features", Arrays.asList(
            "Lead management dashboard",
            "Advanced analytics",
            "Team collaboration"
        )
    ),
    "customer@example.com"
);
```

---

## 🎨 Template Overview

### 1. Subscription Confirmation Email
**Location:** `src/main/resources/templates/email/subscription-confirmation.html`

**Use Case:** Send immediately after successful subscription (checkout completion)

**Variables Required:**
```java
{
    "customerName": "John Doe",           // Customer full name
    "planName": "Professional",           // Plan tier name
    "amount": "$99.00",                   // Monthly/annual amount
    "currency": "USD",                    // Currency code
    "billingFrequency": "Monthly",        // "Monthly" or "Annually"
    "nextBillingDate": "May 5, 2026",    // Next billing date (YYYY-MM-DD)
    "subscriptionId": "sub_1234567890",  // Stripe subscription ID
    "features": [                         // Array of plan features
        "Lead management",
        "Advanced analytics",
        "Team collaboration"
    ]
}
```

**Design Highlights:**
- ✅ Gradient purple header for premium feel
- ✅ Green "Subscription Active" badge
- ✅ Detailed subscription breakdown
- ✅ Feature highlights list
- ✅ Next steps guidance
- ✅ Dashboard CTA button

**Sending Example:**
```java
Map<String, Object> variables = new HashMap<>();
variables.put("customerName", subscription.getCustomerName());
variables.put("planName", subscription.getPlanName());
variables.put("amount", subscription.getAmount());
variables.put("currency", "USD");
variables.put("billingFrequency", "Monthly");
variables.put("nextBillingDate", subscription.getNextBillingDate().toString());
variables.put("subscriptionId", subscription.getId());
variables.put("features", subscription.getPlanFeatures());

emailService.sendEmailTemplate(
    "email/subscription-confirmation",
    variables,
    subscription.getCustomerEmail()
);
```

**Sending Event Hook:**
```java
// In BillingService or CheckoutController
@EventListener
public void onSubscriptionCreated(SubscriptionCreatedEvent event) {
    Map<String, Object> variables = buildSubscriptionVariables(event.getSubscription());
    emailService.sendEmailTemplate(
        "email/subscription-confirmation",
        variables,
        event.getSubscription().getCustomerEmail()
    );
}
```

---

### 2. Payment Failed Alert Email
**Location:** `src/main/resources/templates/email/payment-failed.html`

**Use Case:** Send when payment card is declined, expires, or is insufficient

**Variables Required:**
```java
{
    "customerName": "John Doe",                // Customer name
    "planName": "Professional Plan",           // Plan name
    "amount": "$99.00",                        // Failed charge amount
    "dueDate": "2026-04-05",                  // Original due date
    "paymentMethod": "••••4242",              // Masked card
    "attemptNumber": "1",                     // Current attempt number
    "maxAttempts": "3",                       // Max retry attempts
    "errorMessage": "Card declined by issuer",// Stripe error message
    "errorReasons": [                         // Suggested reasons
        "Insufficient funds",
        "Card expired",
        "Daily limit exceeded"
    ],
    "suspensionDate": "2026-04-15",          // Date account suspends
    "retryHours": "24",                       // Auto-retry in hours
    "supportEmail": "billing@leadflow.com"   // Support contact
}
```

**Design Highlights:**
- ⚠️  Red gradient header for urgency
- ⚠️  "Action Required" badge in red
- ⚠️  Clear error message display
- ⚠️  Specific troubleshooting steps
- ⚠️  Suspension timeline
- ⚠️  Update payment CTA button

**Sending Example:**
```java
// When payment fails
@EventListener
public void onPaymentFailed(PaymentFailedEvent event) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("customerName", event.getCustomerName());
    variables.put("planName", event.getPlanName());
    variables.put("amount", event.getAmount());
    variables.put("dueDate", event.getDueDate().toString());
    variables.put("paymentMethod", maskCardNumber(event.getCardNumber()));
    variables.put("attemptNumber", String.valueOf(event.getAttemptNumber()));
    variables.put("maxAttempts", "3");
    variables.put("errorMessage", event.getStripeErrorMessage());
    variables.put("errorReasons", Arrays.asList(
        "Card declined",
        "Expired card",
        "Insufficient funds",
        "Exceeds daily limit"
    ));
    variables.put("suspensionDate", event.getSuspensionDate().toString());
    variables.put("retryHours", "24");
    variables.put("supportEmail", "billing@leadflow.com");
    
    emailService.sendEmailTemplate(
        "email/payment-failed",
        variables,
        event.getCustomerEmail()
    );
}
```

---

### 3. Invoice Notification Email
**Location:** `src/main/resources/templates/email/invoice-notification.html`

**Use Case:** Send immediately after successful payment

**Variables Required:**
```java
{
    "customerName": "John Doe",               // Customer name
    "customerEmail": "john@example.com",     // For invoice
    "customerCompany": "Acme Inc.",          // Company name if B2B
    "invoiceNumber": "INV-2026-001234",     // Invoice ID
    "invoiceDate": "2026-04-05",            // Invoice date
    "dueDate": "2026-04-30",                // Payment due date
    "paidDate": "2026-04-05",               // Actual payment date
    "paymentMethod": "Visa ••••4242",       // Card used
    "transactionId": "ch_1234567890abc",   // Stripe charge ID
    "lineItems": [                          // Array of items
        {
            "description": "Professional Plan",
            "period": "March 5 - April 5, 2026",
            "quantity": "1",
            "amount": "$99.00"
        }
    ],
    "subtotal": "$99.00",                   // Before tax
    "taxAmount": "$9.90",                   // Tax amount (optional)
    "total": "$108.90",                     // After tax
    "notes": "Thank you for your business"  // Optional notes
}
```

**Design Highlights:**
- ✅ Professional invoice layout
- ✅ Gradient purple header
- ✅ Green "Paid" status badge
- ✅ Itemized line items
- ✅ Tax calculations
- ✅ Transaction details
- ✅ Download receipt CTA

**Sending Example:**
```java
@EventListener
public void onPaymentSuccessful(PaymentCompletedEvent event) {
    Invoice invoice = invoiceService.createInvoice(event);
    
    Map<String, Object> variables = new HashMap<>();
    variables.put("customerName", invoice.getCustomerName());
    variables.put("customerEmail", invoice.getCustomerEmail());
    variables.put("customerCompany", invoice.getCompany());
    variables.put("invoiceNumber", invoice.getId());
    variables.put("invoiceDate", invoice.getCreatedAt().toString());
    variables.put("dueDate", invoice.getDueDate().toString());
    variables.put("paidDate", invoice.getPaidAt().toString());
    variables.put("paymentMethod", maskCardNumber(event.getCard()));
    variables.put("transactionId", event.getTransactionId());
    
    List<Map<String, String>> lineItems = new ArrayList<>();
    Map<String, String> item = new HashMap<>();
    item.put("description", invoice.getDescription());
    item.put("period", formatPeriod(invoice.getPeriodStart(), invoice.getPeriodEnd()));
    item.put("quantity", "1");
    item.put("amount", formatPrice(invoice.getAmount()));
    lineItems.add(item);
    variables.put("lineItems", lineItems);
    
    variables.put("subtotal", formatPrice(invoice.getSubtotal()));
    variables.put("taxAmount", formatPrice(invoice.getTax()));
    variables.put("total", formatPrice(invoice.getTotal()));
    variables.put("notes", "Thank you for your subscription!");
    
    emailService.sendEmailTemplate(
        "email/invoice-notification",
        variables,
        invoice.getCustomerEmail()
    );
}
```

---

### 4. Refund Notification Email
**Location:** `src/main/resources/templates/email/refund-notification.html`

**Use Case:** Send when refund is processed (full or partial)

**Variables Required:**
```java
{
    "customerName": "John Doe",                    // Customer name
    "refundId": "ref_1234567890abc",             // Refund ID
    "originalChargeId": "ch_1234567890abc",      // Original charge
    "invoiceNumber": "INV-2026-001234",          // Related invoice
    "refundAmount": "$99.00",                     // Refund amount
    "refundType": "Full Refund",                 // "Full" or "Partial"
    "originalPaymentDate": "2026-04-05",        // Original payment date
    "refundReason": "Customer requested",        // Why refund happened
    "subscriptionStatus": "Cancelled",           // New subscription state
    "cancellationDate": "2026-04-05",           // When cancelled
    "accessUntilDate": "2026-05-05"             // Last access date
}
```

**Design Highlights:**
- ✅ Gradient green header for positive feel
- ✅ Large refund amount display
- ✅ Clear timeline (1-3 days, 3-5 days)
- ✅ Reason explanation
- ✅ Data retention info
- ✅ FAQ section
- ✅ Reactivation CTA button

**Sending Example:**
```java
@EventListener
public void onRefundProcessed(RefundProcessedEvent event) {
    Refund refund = event.getRefund();
    Subscription subscription = refund.getSubscription();
    
    Map<String, Object> variables = new HashMap<>();
    variables.put("customerName", subscription.getCustomerName());
    variables.put("refundId", refund.getId());
    variables.put("originalChargeId", refund.getOriginalChargeId());
    variables.put("invoiceNumber", refund.getInvoiceId());
    variables.put("refundAmount", formatPrice(refund.getAmount()));
    variables.put("refundType", refund.isFullRefund() ? "Full Refund" : "Partial Refund");
    variables.put("originalPaymentDate", refund.getOriginalPaymentDate().toString());
    variables.put("refundReason", refund.getReason());
    variables.put("subscriptionStatus", "Cancelled");
    variables.put("cancellationDate", subscription.getCancelledAt().toString());
    variables.put("accessUntilDate", subscription.getAccessUntilDate().toString());
    
    emailService.sendEmailTemplate(
        "email/refund-notification",
        variables,
        subscription.getCustomerEmail()
    );
}
```

---

## 🎨 Customization Guide

### Colors
All templates use a consistent color scheme:
```
Primary: #667eea (Purple)
Secondary: #764ba2 (Dark Purple)
Success: #4caf50 (Green)
Error: #d32f2f (Red)
Warning: #ff9800 (Orange)
Info: #1976d2 (Blue)
Text: #333 (Dark Gray)
```

### Change Color Scheme
1. Open template HTML file
2. Find `<style>` section
3. Replace hex colors:
```css
.header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    /* Change to: */
    background: linear-gradient(135deg, #YOUR_COLOR1 0%, #YOUR_COLOR2 100%);
}
```

### Add Company Logo
```html
<div class="header">
    <img src="https://your-domain.com/logo.png" alt="LeadFlow" style="height: 50px; margin-bottom: 20px;">
    <h1>✓ Subscription Confirmed</h1>
</div>
```

### Localization (Multi-language)
Create language-specific templates:
```
templates/
├── email/
│   └── subscription-confirmation.html         (English - default)
├── email/pt/
│   └── subscription-confirmation.html         (Portuguese)
├── email/es/
│   └── subscription-confirmation.html         (Spanish)
└── email/fr/
    └── subscription-confirmation.html         (French)
```

Send to correct template:
```java
String locale = customer.getPreferredLanguage(); // "pt", "es", "fr", "en"
String templatePath = locale.equals("en") ? 
    "email/subscription-confirmation" :
    String.format("email/%s/subscription-confirmation", locale);

emailService.sendEmailTemplate(templatePath, variables, customerEmail);
```

### Add Custom Footer
Edit footer section in any template:
```html
<div class="footer">
    <p>© 2026 LeadFlow. All rights reserved.</p>
    <p>
        <a href="https://leadflow.com/account/settings">Account Settings</a> |
        <a href="https://leadflow.com/privacy">Privacy Policy</a> |
        <a href="https://leadflow.com/terms">Terms of Service</a> |
        <a href="https://leadflow.com/contact">Contact Us</a>
    </p>
    <!-- Add your custom footer here -->
    <p>Address: 123 Main St, San Francisco, CA 94102</p>
</div>
```

---

## 🧪 Testing Templates

### Test in Development
```java
@SpringBootTest
public class EmailTemplateTest {
    
    @Autowired
    private EmailService emailService;
    
    @Test
    public void testSubscriptionConfirmationEmail() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", "Test User");
        variables.put("planName", "Professional");
        variables.put("amount", "$99.00");
        // ... add other variables ...
        
        String html = emailService.renderTemplate(
            "email/subscription-confirmation",
            variables
        );
        
        assert(html.contains("Test User"));
        assert(html.contains("Professional"));
        assert(html.contains("$99.00"));
    }
}
```

### Preview Templates Locally
```bash
# Start application
mvn spring-boot:run

# Open email in browser (simulate)
# See Thymeleaf render at /api/email/preview?template=subscription-confirmation
```

### Email Client Testing
Send test emails to:
1. **Gmail** - Modern email client
2. **Outlook** - Windows/Web email
3. **Apple Mail** - Mac/iOS
4. **Thunderbird** - Desktop client

Check rendering in each to ensure compatibility.

---

## 📊 Best Practices

### Do's ✅
- ✅ Use responsive design (works on mobile, tablet, desktop)
- ✅ Include company branding/logo
- ✅ Make CTA buttons prominent and obvious
- ✅ Use high contrast colors for readability
- ✅ Test on multiple email clients
- ✅ Keep emails under 100KB
- ✅ Include unsubscribe link (required for transactional)
- ✅ Use clear, scannable formatting
- ✅ Include support contact information
- ✅ Test variables are correctly populated

### Don'ts ❌
- ❌ Don't use images as sole content (spam)
- ❌ Don't use too many fonts/colors
- ❌ Don't make emails too wide (max 600px)
- ❌ Don't use JavaScript (not supported)
- ❌ Don't use external stylesheets
- ❌ Don't auto-play audio/video
- ❌ Don't send without testing first
- ❌ Don't include sensitive data in plain text
- ❌ Don't send unsolicited emails

---

## 🔧 Integration Points

### BillingController
```java
@PostMapping("/checkout/success")
public void handleCheckoutSuccess(@RequestBody CheckoutEvent event) {
    // Create subscription
    Subscription subscription = billingService.createSubscription(event);
    
    // Send confirmation email
    publishEvent(new SubscriptionCreatedEvent(subscription));
}
```

### EmailAlertService
```java
@Service
public class EmailAlertService {
    
    public void alertHighWebhookErrorRate(
        double errorRate,
        long errorCount,
        String details
    ) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("errorRate", String.format("%.2f%%", errorRate * 100));
        vars.put("errorCount", errorCount);
        vars.put("timestamp", Instant.now());
        
        sendEmailTemplate(
            "email/alert-high-error-rate",
            vars,
            alertEmailTo
        );
    }
}
```

### ScheduledTasks
```java
@Component
public class EmailScheduler {
    
    @Scheduled(cron = "0 9 * * *") // 9 AM daily
    public void sendDailyInvoiceReminders() {
        List<Invoice> pending = invoiceService.getPendingInvoices();
        for (Invoice invoice : pending) {
            Map<String, Object> vars = buildInvoiceVariables(invoice);
            emailService.sendEmailTemplate(
                "email/invoice-reminder",
                vars,
                invoice.getCustomerEmail()
            );
        }
    }
}
```

---

## 📈 Monitoring Email Delivery

### Track Sent Emails
```sql
SELECT 
    email_type,
    COUNT(*) as count,
    AVG(send_time_ms) as avg_send_time,
    SUM(CASE WHEN status='failed' THEN 1 ELSE 0 END) as failures
FROM emails_sent
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY email_type;
```

### Common Issues
1. **Emails in spam** - Check SPF/DKIM records
2. **Slow delivery** - Monitor SMTP queue
3. **Template rendering fails** - Check variable names
4. **Images not loading** - Use absolute URLs
5. **High bounce rate** - Validate email addresses

---

## 📚 Resources

- [Thymeleaf Documentation](https://www.thymeleaf.org/)
- [Email Client Best Practices](https://litmus.com/blog/)
- [Stripe Email Events](https://stripe.com/docs/webhooks)
- [SMTP Configuration](./SMTP_SETUP_GUIDE.md)
- [Alert Setup](./ALERTING_SETUP.md)

---

_Last updated: March 2026_
