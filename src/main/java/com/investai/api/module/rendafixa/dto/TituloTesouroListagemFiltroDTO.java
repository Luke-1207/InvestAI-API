package com.investai.api.module.rendafixa.dto;

import com.investai.api.module.rendafixa.entity.TipoTesouro;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TituloTesouroListagemFiltroDTO {
    private TipoTesouro tipo;
    private LocalDate vencimentoAte;
    private BigDecimal taxaMinima;
    private OrdenarPorTesouro ordenarPor = OrdenarPorTesouro.VENCIMENTO;
    private int pagina = 1;
    private int tamanho = 20;
}