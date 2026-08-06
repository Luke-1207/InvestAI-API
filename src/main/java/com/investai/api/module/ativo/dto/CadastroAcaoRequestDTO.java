package com.investai.api.module.ativo.dto;

import com.investai.api.module.ativo.entity.TipoAtivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CadastroAcaoRequestDTO {
    @NotBlank(message = "Código é obrigatório")
    @Size(max = 10, message = "Código deve ter no máximo 10 caracteres")
    private String codigo;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
    private String nome;

    @NotNull(message = "Tipo é obrigatório")
    private TipoAtivo tipo;

    @Size(max = 100, message = "Setor deve ter no máximo 100 caracteres")
    private String setor;
}
