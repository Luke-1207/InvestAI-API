package com.investai.api.module.notificacao.controller;

import com.investai.api.module.notificacao.dto.ComunicadoSistemaRequestDTO;
import com.investai.api.module.notificacao.dto.ComunicadoSistemaResponseDTO;
import com.investai.api.module.notificacao.dto.ContagemNaoLidasResponseDTO;
import com.investai.api.module.notificacao.dto.MarcarTodasLidasResponseDTO;
import com.investai.api.module.notificacao.dto.NotificacaoResponseDTO;
import com.investai.api.module.notificacao.service.NotificacaoService;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @GetMapping
    public ResponseEntity<Page<NotificacaoResponseDTO>> listar(
            @RequestParam(required = false) Boolean lida,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok(notificacaoService.listar(usuarioAutenticadoHelper.getIdUsuarioLogado(), lida, pagina, tamanho));
    }

    @GetMapping("/nao-lidas/contagem")
    public ResponseEntity<ContagemNaoLidasResponseDTO> contarNaoLidas() {
        return ResponseEntity.ok(notificacaoService.contarNaoLidas(usuarioAutenticadoHelper.getIdUsuarioLogado()));
    }

    @PatchMapping("/{id}/lida")
    public ResponseEntity<NotificacaoResponseDTO> marcarComoLida(@PathVariable UUID id) {
        return ResponseEntity.ok(notificacaoService.marcarComoLida(usuarioAutenticadoHelper.getIdUsuarioLogado(), id));
    }

    @PatchMapping("/lidas")
    public ResponseEntity<MarcarTodasLidasResponseDTO> marcarTodasComoLidas() {
        return ResponseEntity.ok(notificacaoService.marcarTodasComoLidas(usuarioAutenticadoHelper.getIdUsuarioLogado()));
    }

    @PostMapping("/sistema")
    public ResponseEntity<ComunicadoSistemaResponseDTO> enviarComunicado(@Valid @RequestBody ComunicadoSistemaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificacaoService.enviarComunicado(dto));
    }
}
