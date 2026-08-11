package com.investai.api.infra.hgbrasil.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HgBrasilHistoricalResponseDTO {

    private String by;

    @JsonProperty("sample_by")
    private String sampleBy;

    @JsonProperty("valid_key")
    private Boolean validKey;

    private Map<String, Map<String, HgBrasilCandleDTO>> results;
}