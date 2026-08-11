package com.investai.api.infra.hgbrasil.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HgBrasilCandleDTO {
    private Double open;
    private Double close;
    private Double high;
    private Double low;
    private Long volume;
}
