package com.investai.api.infra.tesourodireto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrapiTreasuryIndicatorsResponseDTO {
    private List<BrapiTreasuryDTO> results;
}