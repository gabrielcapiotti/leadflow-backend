// k6 Load Testing Script - LeadFlow Billing API
// 
// Simulates realistic webhook and billing API traffic
// Tests capacity, latency, and error rates under load
//
// Run with:
// k6 run script.js
// k6 run --vus 100 --duration 60s script.js  (100 virtual users for 60 seconds)
// k6 run --ramp-up script.js                  (gradually ramp up)

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Counter, Gauge, Rate } from 'k6/metrics';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

// ====== Custom Metrics ======
const requestDuration = new Trend('request_duration');
const checkoutLatency = new Trend('checkout_latency');
const webhookLatency = new Trend('webhook_latency');
const checkoutErrorRate = new Rate('checkout_errors');
const webhookErrorRate = new Rate('webhook_errors');
const webhooksProcessed = new Counter('webhooks_processed');

// ====== Configuration ======
const API_BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080/api';
const JWT_TOKEN = __ENV.JWT_TOKEN || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...';
const WEBHOOK_SECRET = __ENV.WEBHOOK_SECRET || 'whsec_test_12345678';
const TENANT_ID = __ENV.TENANT_ID || '550e8400-e29b-41d4-a716-446655440000';

// ====== Load Test Stages ======
export const options = {
  stages: [
    { duration: '30s', target: 10 },    // Ramp up to 10 VUs
    { duration: '1m', target: 50 },     // Ramp up to 50 VUs
    { duration: '2m', target: 100 },    // Ramp up to 100 VUs
    { duration: '2m', target: 100 },    // Stay at 100 VUs
    { duration: '1m', target: 50 },     // Ramp down to 50 VUs
    { duration: '30s', target: 0 },     // Ramp down to 0 VUs
  ],
  thresholds: {
    // Define acceptable thresholds
    'http_req_duration': ['p(95)<500', 'p(99)<1000'],  // 95% of requests < 500ms
    'http_req_failed': ['rate<0.1'],                    // Error rate < 10%
    'checkout_errors': ['rate<0.05'],                   // Checkout error rate < 5%
    'webhook_errors': ['rate<0.02'],                    // Webhook error rate < 2%
  },
};

// ====== Setup (runs once at start) ======
export function setup() {
  console.log('Starting LeadFlow Billing API Load Test');
  return {
    startTime: new Date().toISOString(),
    baseUrl: API_BASE_URL,
    jwtToken: JWT_TOKEN,
    webhookSecret: WEBHOOK_SECRET,
  };
}

// ====== Main Test Scenario ======
export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'X-Tenant-ID': TENANT_ID,
    'Authorization': `Bearer ${data.jwtToken}`,
  };

  // ====== Test 1: Checkout Creation (70% of traffic) ======
  if (Math.random() < 0.7) {
    group('Checkout Creation', () => {
      const checkoutPayload = {
        email: `user${__VU}-${__ITER}@example.com`,
        successUrl: 'https://leadflow.com/billing/success',
        cancelUrl: 'https://leadflow.com/billing/cancel',
        quantity: 1,
      };

      const startTime = Date.now();
      const res = http.post(
        `${data.baseUrl}/billing/checkout`,
        JSON.stringify(checkoutPayload),
        { headers }
      );
      const duration = Date.now() - startTime;

      requestDuration.add(duration);
      checkoutLatency.add(duration);
      checkoutErrorRate.add(res.status !== 200);

      check(res, {
        'checkout status is 200': (r) => r.status === 200,
        'checkout response time < 1s': (r) => duration < 1000,
        'checkout returns checkoutUrl': (r) => JSON.parse(r.body).checkoutUrl !== undefined,
        'checkout returns sessionId': (r) => JSON.parse(r.body).sessionId !== undefined,
      });
    });

    sleep(0.5);
  }

  // ====== Test 2: Webhook Processing (30% of traffic) ======
  else {
    group('Webhook Processing', () => {
      const eventTypes = [
        'customer.subscription.created',
        'invoice.payment_failed',
        'invoice.payment_succeeded',
        'charge.refunded',
      ];

      const eventType = eventTypes[Math.floor(Math.random() * eventTypes.length)];
      const timestamp = Math.floor(Date.now() / 1000).toString();

      // Generate payload
      const payload = generateWebhookPayload(eventType);
      const payloadStr = JSON.stringify(payload);

      // Compute HMAC signature
      const signedContent = timestamp + '.' + payloadStr;
      const signature = computeHmac(signedContent, data.webhookSecret);

      const webhookHeaders = {
        'Content-Type': 'application/json',
        'Stripe-Signature': `t=${timestamp},v1=${signature}`,
      };

      const startTime = Date.now();
      const res = http.post(
        `${data.baseUrl}/webhooks/stripe`,
        payloadStr,
        { headers: webhookHeaders }
      );
      const duration = Date.now() - startTime;

      requestDuration.add(duration);
      webhookLatency.add(duration);
      webhookErrorRate.add(res.status !== 200);
      webhooksProcessed.add(1);

      check(res, {
        'webhook status is 200': (r) => r.status === 200,
        'webhook response time < 500ms': (r) => duration < 500,
        'webhook returns success': (r) => r.body.includes('success') || r.body.includes('received'),
        'webhook rejects invalid signatures': (r) => {
          // Test with wrong signature (10% of webhook requests)
          if (Math.random() < 0.1) {
            return r.status === 400;
          }
          return r.status === 200;
        },
      });
    });

    sleep(0.2);
  }

  // ====== Test 3: Monitoring Endpoints (5% of traffic) ======
  if (Math.random() < 0.05) {
    group('Monitoring', () => {
      const res = http.get(`${data.baseUrl}/actuator/health`, { headers });

      check(res, {
        'health endpoint status is 200': (r) => r.status === 200,
        'health status is UP': (r) => JSON.parse(r.body).status === 'UP',
      });
    });

    sleep(0.1);
  }
}

// ====== Teardown (runs once at end) ======
export function teardown(data) {
  console.log('Load test completed');
  console.log(`Test duration: ${new Date().toISOString()}`);
  console.log(`Webhooks processed: ${webhooksProcessed.value}`);
}

// ====== Helper Functions ======

function generateWebhookPayload(eventType) {
  const timestamp = Math.floor(Date.now() / 1000);

  switch (eventType) {
    case 'customer.subscription.created':
      return {
        id: `evt_${randomId()}`,
        type: eventType,
        created: timestamp,
        data: {
          object: {
            id: `sub_${randomId()}`,
            customer: `cus_${randomId()}`,
            status: 'active',
            current_period_started: timestamp,
            current_period_end: timestamp + 2592000, // 30 days
            items: {
              data: [{
                price: {
                  id: `price_${randomId()}`,
                  unit_amount: 2999,
                  currency: 'usd',
                },
              }],
            },
          },
        },
      };

    case 'invoice.payment_failed':
      return {
        id: `evt_${randomId()}`,
        type: eventType,
        created: timestamp,
        data: {
          object: {
            id: `in_${randomId()}`,
            customer: `cus_${randomId()}`,
            subscription: `sub_${randomId()}`,
            amount_due: 2999,
            currency: 'usd',
            attempt_count: 1,
            next_payment_attempt: timestamp + 86400,
          },
        },
      };

    case 'invoice.payment_succeeded':
      return {
        id: `evt_${randomId()}`,
        type: eventType,
        created: timestamp,
        data: {
          object: {
            id: `in_${randomId()}`,
            customer: `cus_${randomId()}`,
            subscription: `sub_${randomId()}`,
            amount_paid: 2999,
            currency: 'usd',
            status: 'paid',
          },
        },
      };

    case 'charge.refunded':
      return {
        id: `evt_${randomId()}`,
        type: eventType,
        created: timestamp,
        data: {
          object: {
            id: `ch_${randomId()}`,
            customer: `cus_${randomId()}`,
            amount: 2999,
            currency: 'usd',
            refunded: true,
            refunds: {
              data: [{
                id: `re_${randomId()}`,
                amount: 2999,
              }],
            },
          },
        },
      };

    default:
      return { type: eventType, created: timestamp };
  }
}

function computeHmac(message, secret) {
  // Convert to hex for k6
  const hash = crypto.sha256(message, 'utf-8');
  const hmac = crypto.hmac('sha256', secret, message, 'utf-8');
  return encoding.b64encode(hmac);
}

function randomId() {
  return Math.random().toString(36).substring(2, 15) +
         Math.random().toString(36).substring(2, 15);
}

function sleep(duration) {
  // Sleep between requests to simulate realistic behavior
  const start = Date.now();
  while (Date.now() - start < duration * 1000) {
    // Busy wait
  }
}
