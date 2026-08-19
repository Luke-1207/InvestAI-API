package com.investai.api.infra.tesourodireto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrapiRateInfoDTO {
    private String rateType;
    private String rateUnit;
    private String description;
}