package com.investai.api.shared.scoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriterioScoreDTO {
    private String chave;
    private String descricao;
    private int pontos;
}
