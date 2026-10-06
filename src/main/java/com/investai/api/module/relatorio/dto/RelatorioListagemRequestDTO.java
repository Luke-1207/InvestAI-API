package com.investai.api.module.relatorio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioListagemRequestDTO {

    public static final int LIMITE_ATIVOS = 50;
    public static final int LIMITE_FILTROS = 20;

    @NotNull(message = "O módulo é obrigatório (VARIAVEL, FIXA ou AMBOS)")
    private ModuloRelatorio modulo;

    @Size(max = LIMITE_FILTROS, message = "Informe no máximo " + LIMITE_FILTROS + " filtros")
    private Map<@NotBlank(message = "O nome do filtro não pode ser vazio") @Size(max = 60) String,
            @Size(max = 120, message = "O valor de um filtro pode ter no máximo 120 caracteres") String> filtros;

    @Size(max = LIMITE_ATIVOS, message = "Informe no máximo " + LIMITE_ATIVOS + " ativos por relatório")
    private List<@NotBlank(message = "O código do ativo não pode ser vazio") String> ativos;
}
