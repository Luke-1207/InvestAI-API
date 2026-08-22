package com.investai.api.infra.hgbrasil.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HgBrasilStockIndexDTO {
    private String name;
    private BigDecimal points;
    private BigDecimal variation;
}