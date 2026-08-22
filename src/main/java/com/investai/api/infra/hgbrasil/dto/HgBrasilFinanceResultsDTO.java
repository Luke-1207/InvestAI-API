package com.investai.api.infra.hgbrasil.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HgBrasilFinanceResultsDTO {
    private HgBrasilCurrenciesDTO currencies;
    private Map<String, HgBrasilStockIndexDTO> stocks;
}