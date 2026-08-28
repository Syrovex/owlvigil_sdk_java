package com.owlvigil.management;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class UsageSummary {
    @JsonProperty("workspace_id") public long workspaceId;
    public long requests;
    public long tokens;
    public BigDecimal cost;
    @JsonProperty("current_period") public String currentPeriod;
    public long workspaceId() { return workspaceId; }
    public long requests() { return requests; }
    public long tokens() { return tokens; }
    public BigDecimal cost() { return cost; }
    public String currentPeriod() { return currentPeriod; }
}
