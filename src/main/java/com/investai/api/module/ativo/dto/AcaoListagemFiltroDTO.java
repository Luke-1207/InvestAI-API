package com.investai.api.module.ativo.dto;

import com.investai.api.module.ativo.entity.TipoAtivo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AcaoListagemFiltroDTO {
    private List<TipoAtivo> tipo;
    private String setor;
    private BigDecimal dyMinimo;
    private BigDecimal precoMaximo;
    private OrdenarPorAcao ordenarPor = OrdenarPorAcao.NOME;
    private OrdemDTO ordem = OrdemDTO.ASC;
    private int pagina = 1;
    private int tamanho = 20;
}
