package com.leadflow.backend.webhook.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark for Stripe webhook HMAC-SHA256 validation
 * 
 * Measures:
 * - Time to compute HMAC signature
 * - Time to validate signature against computed value
 * - Impact of constant-time comparison
 * - Various payload sizes
 * 
 * Run with: mvn test -Dtest=StripeWebhookHmacBenchmark
 */
@Fork(value = 2, jvmArgs = {"-XX:+UseG1GC", "-Xmx1G"})
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class StripeWebhookHmacBenchmark {

    private static final String SECRET = "whsec_test_" + "x".repeat(100);
    private static final String TIMESTAMP = "1234567890";
    
    @Param({"100", "500", "1000", "5000"})
    private int payloadSize;
    
    private String payload;
    private String signature;
    private String signedContent;

    @Setup(Level.Trial)
    public void setupTrial() throws Exception {
        // Generate payload of varying sizes
        StringBuilder sb = new StringBuilder("{\"type\":\"test\",\"data\":{");
        for (int i = 0; i < payloadSize; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"field").append(i).append("\":\"value").append(i).append("\"");
        }
        sb.append("}}");
        payload = sb.toString();
        
        // Pre-compute signed content
        signedContent = TIMESTAMP + "." + payload;
        
        // Pre-compute signature
        signature = computeHmacSha256(signedContent, SECRET);
    }

    /**
     * Benchmark: Computing HMAC-SHA256 signature
     */
    @Benchmark
    public String benchmarkComputeHmac(Blackhole bh) throws Exception {
        return computeHmacSha256(signedContent, SECRET);
    }

    /**
     * Benchmark: Verifying signature with constant-time comparison
     */
    @Benchmark
    public boolean benchmarkVerifySignature(Blackhole bh) throws Exception {
        String computed = computeHmacSha256(signedContent, SECRET);
        return constantTimeEquals(signature, computed);
    }

    /**
     * Benchmark: Standard string comparison (vulnerable to timing attacks)
     */
    @Benchmark
    public boolean benchmarkNaiveComparison(Blackhole bh) {
        String computed = tryComputeHmac();
        return signature.equals(computed);
    }

    /**
     * Benchmark: Payload parsing (JSON parsing overhead)
     */
    @Benchmark
    public String benchmarkPayloadExtraction(Blackhole bh) {
        // Simulates extracting data from webhook payload
        int startIdx = payload.indexOf("\"type\":");
        int endIdx = payload.indexOf(",", startIdx);
        return payload.substring(startIdx, endIdx);
    }

    /**
     * Benchmark: Full webhook processing (compute + verify + parse)
     */
    @Benchmark
    public boolean benchmarkFullValidation(Blackhole bh) throws Exception {
        // 1. Compute expected signature
        String computedSig = computeHmacSha256(signedContent, SECRET);
        
        // 2. Compare with provided signature
        boolean isValid = constantTimeEquals(signature, computedSig);
        
        // 3. Verify timestamp is recent (simulated)
        long ts = Long.parseLong(TIMESTAMP);
        long age = System.currentTimeMillis() / 1000 - ts;
        boolean isRecent = age < 300;
        
        return isValid && isRecent;
    }

    /**
     * Benchmark: Different payload sizes impact on HMAC speed
     * (parameterized with @Param)
     */
    @Benchmark
    public String benchmarkPayloadSizeImpact(Blackhole bh) throws Exception {
        return computeHmacSha256(signedContent, SECRET);
    }

    // ==================== Helper Methods ====================

    private String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        ));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private String tryComputeHmac() {
        try {
            return computeHmacSha256(signedContent, SECRET);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8),
            b.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ==================== Main Method ====================

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(StripeWebhookHmacBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(3)
            .measurementIterations(5)
            .build();

        new Runner(opt).run();
    }
}
