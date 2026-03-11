package com.leadflow.backend.email.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark for email template rendering performance
 * 
 * Measures:
 * - Thymeleaf template rendering overhead
 * - Different email template complexity
 * - Variable substitution speed
 * - Conditional processing in templates
 * 
 * Run with: mvn test -Dtest=EmailRenderingBenchmark
 */
@Fork(value = 2, jvmArgs = {"-XX:+UseG1GC", "-Xmx1G"})
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class EmailRenderingBenchmark {

    private TemplateEngine templateEngine;
    private Context confirmationContext;
    private Context failureContext;
    private Context invoiceContext;

    @Setup(Level.Trial)
    public void setupTrial() {
        // Initialize Thymeleaf (simplified - in real test would use Spring context)
        templateEngine = createTemplateEngine();
        
        // Prepare contexts
        confirmationContext = createConfirmationContext();
        failureContext = createFailureContext();
        invoiceContext = createInvoiceContext();
    }

    /**
     * Benchmark: Render subscription confirmation email
     */
    @Benchmark
    public String benchmarkRenderConfirmationEmail(Blackhole bh) throws Exception {
        return templateEngine.process("email/subscription-confirmation", confirmationContext);
    }

    /**
     * Benchmark: Render payment failed email
     */
    @Benchmark
    public String benchmarkRenderFailureEmail(Blackhole bh) throws Exception {
        return templateEngine.process("email/payment-failed", failureContext);
    }

    /**
     * Benchmark: Render invoice email
     */
    @Benchmark
    public String benchmarkRenderInvoiceEmail(Blackhole bh) throws Exception {
        return templateEngine.process("email/invoice", invoiceContext);
    }

    /**
     * Benchmark: Context variable insertion (simple substitution)
     */
    @Benchmark
    public Map<String, Object> benchmarkContextCreation(Blackhole bh) {
        Map<String, Object> context = new HashMap<>();
        context.put("recipientName", "John Doe");
        context.put("subscriptionStatus", "active");
        context.put("nextBillingDate", "2026-03-10");
        context.put("amount", "$29.99");
        context.put("currency", "USD");
        context.put("supportEmail", "support@leadflow.com");
        context.put("companyName", "LeadFlow");
        return context;
    }

    /**
     * Benchmark: Simple string template rendering
     */
    @Benchmark
    public String benchmarkSimpleStringTemplate(Blackhole bh) {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "John");
        vars.put("date", "2026-03-10");
        
        String template = "Hello ${name}, your subscription starts on ${date}";
        String result = template
            .replace("${name}", vars.get("name"))
            .replace("${date}", vars.get("date"));
        
        return result;
    }

    /**
     * Benchmark: HTML generation with conditions
     */
    @Benchmark
    public StringBuilder benchmarkHtmlGeneration(Blackhole bh) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h1>Subscription Confirmation</h1>");
        html.append("<p>Dear John Doe,</p>");
        html.append("<p>Your subscription has been confirmed.</p>");
        html.append("<p>Amount: $29.99/month</p>");
        
        boolean isPro = true;
        if (isPro) {
            html.append("<p>Plus: Pro features enabled</p>");
        }
        
        html.append("<p>Questions? <a href=\"mailto:support@leadflow.com\">Contact us</a></p>");
        html.append("</body></html>");
        
        return html;
    }

    /**
     * Benchmark: Multiple email rendering (batch processing)
     */
    @Benchmark
    public int benchmarkBatchEmailRendering(Blackhole bh) throws Exception {
        int rendered = 0;
        
        for (int i = 0; i < 10; i++) {
            Context ctx = new Context();
            ctx.setVariable("recipientName", "User" + i);
            ctx.setVariable("index", i);
            
            String html = templateEngine.process("email/subscription-confirmation", ctx);
            if (!html.isEmpty()) rendered++;
        }
        
        return rendered;
    }

    /**
     * Benchmark: Email with complex data structures
     */
    @Benchmark
    public String benchmarkComplexEmailRendering(Blackhole bh) throws Exception {
        Context context = new Context();
        
        // Simulate complex nested data
        Map<String, Object> subscription = new HashMap<>();
        subscription.put("id", "sub_1234567890");
        subscription.put("status", "active");
        
        Map<String, Object> plan = new HashMap<>();
        plan.put("name", "Professional");
        plan.put("amount", 2999);
        plan.put("interval", "month");
        subscription.put("plan", plan);
        
        context.setVariable("subscription", subscription);
        context.setVariable("invoices", 5);
        context.setVariable("nextBillingDate", "2026-03-10");
        
        return templateEngine.process("email/subscription-confirmation", context);
    }

    // ==================== Helper Methods ====================

    private TemplateEngine createTemplateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private Context createConfirmationContext() {
        Context context = new Context();
        context.setVariable("recipientName", "John Doe");
        context.setVariable("subscriptionStatus", "active");
        context.setVariable("nextBillingDate", "2026-03-10");
        context.setVariable("amount", "29.99");
        context.setVariable("currency", "USD");
        context.setVariable("supportEmail", "support@leadflow.com");
        context.setVariable("companyName", "LeadFlow");
        return context;
    }

    private Context createFailureContext() {
        Context context = new Context();
        context.setVariable("recipientName", "Jane Smith");
        context.setVariable("invoiceAmount", "29.99");
        context.setVariable("cardEnding", "4242");
        context.setVariable("retryDate", "2026-03-08");
        context.setVariable("supportUrl", "https://support.leadflow.com");
        context.setVariable("supportEmail", "support@leadflow.com");
        return context;
    }

    private Context createInvoiceContext() {
        Context context = new Context();
        context.setVariable("invoiceNumber", "INV-2026-001");
        context.setVariable("recipientName", "Bob Johnson");
        context.setVariable("invoiceDate", "2026-03-01");
        context.setVariable("dueDate", "2026-04-01");
        context.setVariable("amount", "29.99");
        context.setVariable("currency", "USD");
        context.setVariable("items", "Professional Plan - 1 month");
        context.setVariable("taxAmount", "0.00");
        context.setVariable("totalAmount", "29.99");
        return context;
    }

    // ==================== Main Method ====================

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(EmailRenderingBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(3)
            .measurementIterations(5)
            .build();

        new Runner(opt).run();
    }
}
