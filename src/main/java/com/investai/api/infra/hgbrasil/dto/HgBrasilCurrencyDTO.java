package com.investai.api.infra.hgbrasil.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HgBrasilCurrencyDTO {
    private String name;
    private BigDecimal buy;
    private BigDecimal sell;
    private BigDecimal variation;
}