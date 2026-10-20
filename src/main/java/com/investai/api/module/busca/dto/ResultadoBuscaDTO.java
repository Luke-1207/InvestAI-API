package com.investai.api.module.busca.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResultadoBuscaDTO {
    private GrupoBusca grupo;
    private String categoria;
    private String identificador;
    private String codigo;
    private String nome;
    private String detalhe;
}
