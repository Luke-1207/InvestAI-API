package com.investai.api.infra.tesourodireto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrapiTreasuryDTO {
    private String symbol;
    private String bondType;
    private String indexer;
    private String couponType;
    private String maturityDate;
    private Integer durationDays;
    private String baseDate;
    private Double buyRate;
    private Double sellRate;
    private Double buyPrice;
    private Double sellPrice;
    private Double basePrice;
    private BrapiRateInfoDTO rateInfo;
}