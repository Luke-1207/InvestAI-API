package com.investai.api.infra.hgbrasil.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HgBrasilFinanceResponseDTO {
    private Boolean validKey;
    private HgBrasilFinanceResultsDTO results;
}