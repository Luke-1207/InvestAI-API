package com.investai.api.module.relatorio.service;

import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.ativo.dto.PeriodoHistorico;
import com.investai.api.module.ativo.service.AcaoDetalheService;
import com.investai.api.module.ativo.service.AcaoSugestaoService;
import com.investai.api.module.ativo.service.ResumoAtivoService;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.relatorio.builder.RelatorioAtivoBuilder;
import com.investai.api.module.relatorio.dto.DadosRelatorioAtivoVariavel;
import com.investai.api.module.relatorio.dto.RelatorioGeradoDTO;
import com.investai.api.module.perfil.service.PerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final AcaoDetalheService acaoDetalheService;
    private final AcaoSugestaoService acaoSugestaoService;
    private final ResumoAtivoService resumoAtivoService;
    private final PerfilService perfilService;
    private final RelatorioAtivoBuilder relatorioAtivoBuilder;

    public RelatorioGeradoDTO gerarRelatorioAtivoVariavel(String codigo, Usuario usuario) {
        AcaoDetalheResponseDTO ativo = acaoDetalheService.obterDetalhe(codigo, PeriodoHistorico.UM_ANO.getCodigo());

        DadosRelatorioAtivoVariavel dados = DadosRelatorioAtivoVariavel.builder()
                .nomeUsuario(usuario.getNome())
                .geradoEm(LocalDateTime.now())
                .perfil(perfilService.obterPerfil(usuario.getId()))
                .ativo(ativo)
                .compatibilidade(acaoSugestaoService.obterSugestao(ativo.getCodigo(), usuario.getId()))
                .resumoIa(resumoAtivoService.obterResumo(usuario.getId(), ativo).orElse(null))
                .build();

        return new RelatorioGeradoDTO(
                "analise-" + ativo.getCodigo() + ".pdf",
                relatorioAtivoBuilder.construir(dados));
    }
}
