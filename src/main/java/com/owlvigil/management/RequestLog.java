package com.owlvigil.management;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class RequestLog {
    @JsonProperty("request_id") public String requestId;
    public String model;
    public String status;
    @JsonProperty("status_code") public int statusCode;
    @JsonProperty("total_tokens") public long totalTokens;
    @JsonProperty("total_cost") public BigDecimal totalCost;
    public String requestId() { return requestId; }
    public String model() { return model; }
    public String status() { return status; }
    public int statusCode() { return statusCode; }
    public long totalTokens() { return totalTokens; }
    public BigDecimal totalCost() { return totalCost; }
}
