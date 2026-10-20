package com.investai.api.module.busca.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuscaAtivosRequestDTO {

    public static final int LIMITE_PADRAO = 8;

    @NotBlank(message = "Informe o que você quer buscar.")
    @Size(max = 60, message = "A busca pode ter no máximo 60 caracteres.")
    private String termo;

    @Min(value = 1, message = "O limite precisa ser de pelo menos 1 resultado.")
    @Max(value = 20, message = "O limite pode ser de no máximo 20 resultados.")
    private Integer limite = LIMITE_PADRAO;
}
