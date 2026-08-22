package com.investai.api.infra.hgbrasil.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HgBrasilCurrenciesDTO {
    @JsonProperty("USD")
    private HgBrasilCurrencyDTO usd;

    @JsonProperty("EUR")
    private HgBrasilCurrencyDTO eur;
}