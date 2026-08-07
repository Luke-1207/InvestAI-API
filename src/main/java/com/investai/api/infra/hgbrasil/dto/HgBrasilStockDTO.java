package com.investai.api.infra.hgbrasil.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HgBrasilStockDTO {

    private Boolean error;
    private String message;

    private String kind;
    private String symbol;
    private String name;
    private String sector;

    private Double price;

    @JsonProperty("change_percent")
    private Double changePercent;

    @JsonProperty("change_price")
    private Double changePrice;

    @JsonProperty("market_cap")
    private Double marketCap;

    private Long volume;

    @JsonProperty("updated_at")
    private String updatedAt;

    private HgBrasilFinancialsDTO financials;
}
