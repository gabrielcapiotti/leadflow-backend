package com.leadflow.backend.dto.billing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaktoWebhookPayload {

    private String id;
    private String type;
    private Data data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String status;

        @JsonProperty("subscription_id")
        private String subscriptionId;

        private Customer customer;

        @JsonProperty("next_billing_at")
        private Instant nextBillingAt;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getSubscriptionId() {
            return subscriptionId;
        }

        public void setSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
        }

        public Customer getCustomer() {
            return customer;
        }

        public void setCustomer(Customer customer) {
            this.customer = customer;
        }

        public Instant getNextBillingAt() {
            return nextBillingAt;
        }

        public void setNextBillingAt(Instant nextBillingAt) {
            this.nextBillingAt = nextBillingAt;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Customer {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}
