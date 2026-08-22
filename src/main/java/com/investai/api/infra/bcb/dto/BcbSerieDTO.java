package com.investai.api.infra.bcb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BcbSerieDTO {
    private String data;
    private String valor;
}