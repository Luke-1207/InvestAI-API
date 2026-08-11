package com.investai.api.infra.hgbrasil.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HgBrasilFinancialsDTO {

    @JsonProperty("price_to_book_ratio")
    private Double priceToBookRatio;

    private HgBrasilDividendsDTO dividends;
}
