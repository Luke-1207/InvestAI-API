package com.investai.api.module.relatorio.controller;

import com.investai.api.infra.rabbitmq.dto.ModuloIa;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.relatorio.dto.HistoricoRelatorioResponseDTO;
import com.investai.api.module.relatorio.dto.RelatorioGeradoDTO;
import com.investai.api.module.relatorio.dto.RelatorioListagemRequestDTO;
import com.investai.api.module.relatorio.service.HistoricoRelatorioService;
import com.investai.api.module.relatorio.service.RelatorioService;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;
    private final HistoricoRelatorioService historicoRelatorioService;
    private final UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @GetMapping("/ativo/{identificador}")
    public ResponseEntity<byte[]> gerarRelatorioAtivo(
            @PathVariable String identificador,
            @RequestParam(defaultValue = "VARIAVEL") ModuloIa tipo
    ) {
        Usuario usuario = usuarioAutenticadoHelper.getUsuarioLogado();
        RelatorioGeradoDTO relatorio = tipo == ModuloIa.FIXA
                ? relatorioService.gerarRelatorioAtivoFixo(identificador, usuario)
                : relatorioService.gerarRelatorioAtivoVariavel(identificador, usuario);

        return comoDownload(relatorio);
    }

    @PostMapping("/listagem")
    public ResponseEntity<byte[]> gerarRelatorioListagem(@Valid @RequestBody RelatorioListagemRequestDTO request) {
        return comoDownload(relatorioService.gerarRelatorioListagem(
                request, usuarioAutenticadoHelper.getUsuarioLogado()));
    }

    @GetMapping("/perfil")
    public ResponseEntity<byte[]> gerarRelatorioPerfil() {
        return comoDownload(relatorioService.gerarRelatorioPerfil(usuarioAutenticadoHelper.getUsuarioLogado()));
    }

    @GetMapping("/historico")
    public ResponseEntity<Page<HistoricoRelatorioResponseDTO>> listarHistorico(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho
    ) {
        return ResponseEntity.ok(historicoRelatorioService.listar(
                usuarioAutenticadoHelper.getIdUsuarioLogado(), pagina, tamanho));
    }

    private ResponseEntity<byte[]> comoDownload(RelatorioGeradoDTO relatorio) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(relatorio.nomeArquivo()).build().toString())
                .body(relatorio.conteudo());
    }
}
