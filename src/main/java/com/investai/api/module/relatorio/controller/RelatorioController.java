package com.investai.api.module.relatorio.controller;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.rabbitmq.dto.ModuloIa;
import com.investai.api.module.relatorio.dto.RelatorioGeradoDTO;
import com.investai.api.module.relatorio.service.RelatorioService;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;
    private final UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @GetMapping("/ativo/{codigo}")
    public ResponseEntity<byte[]> gerarRelatorioAtivo(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "VARIAVEL") ModuloIa tipo
    ) {
        if (tipo != ModuloIa.VARIAVEL) {
            // TODO INVAI-63: relatório de ativo de renda fixa
            throw new BusinessException("Relatório de renda fixa ainda não está disponível");
        }

        RelatorioGeradoDTO relatorio = relatorioService.gerarRelatorioAtivoVariavel(
                codigo, usuarioAutenticadoHelper.getUsuarioLogado());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(relatorio.nomeArquivo()).build().toString())
                .body(relatorio.conteudo());
    }
}
