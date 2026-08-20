package com.investai.api.module.rendafixa.dto;

import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TituloPrivadoListagemFiltroDTO {
    private List<TipoTituloPrivado> tipo;
    private Indexador indexador;
    private TipoLiquidez liquidez;
    private Boolean isentoIR;
    private BigDecimal investimentoMaximo;
    private LocalDate vencimentoAte;
    private OrdenarPorTituloPrivado ordenarPor = OrdenarPorTituloPrivado.TAXA;
    private int pagina = 1;
    private int tamanho = 20;
}