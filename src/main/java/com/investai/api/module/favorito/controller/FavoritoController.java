package com.investai.api.module.favorito.controller;

import com.investai.api.module.favorito.dto.AdicionarFavoritoRequestDTO;
import com.investai.api.module.favorito.dto.ConfigurarAlertasRequestDTO;
import com.investai.api.module.favorito.dto.FavoritoResponseDTO;
import com.investai.api.module.favorito.dto.FavoritoStatusResponseDTO;
import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.favorito.service.FavoritoService;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/favoritos")
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;
    private final UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @GetMapping
    public ResponseEntity<List<FavoritoResponseDTO>> listar() {
        return ResponseEntity.ok(favoritoService.listar(usuarioAutenticadoHelper.getIdUsuarioLogado()));
    }

    @PostMapping
    public ResponseEntity<FavoritoResponseDTO> adicionar(@Valid @RequestBody AdicionarFavoritoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(favoritoService.adicionar(usuarioAutenticadoHelper.getIdUsuarioLogado(), dto));
    }

    @GetMapping("/status")
    public ResponseEntity<FavoritoStatusResponseDTO> status(
            @RequestParam TipoAtivoFavorito tipoAtivo,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) UUID ativoId) {
        return ResponseEntity.ok(favoritoService.status(usuarioAutenticadoHelper.getIdUsuarioLogado(), tipoAtivo, codigo, ativoId));
    }

    @PutMapping("/{id}/alertas")
    public ResponseEntity<FavoritoResponseDTO> configurarAlertas(
            @PathVariable UUID id,
            @Valid @RequestBody ConfigurarAlertasRequestDTO dto) {
        return ResponseEntity.ok(favoritoService.configurarAlertas(usuarioAutenticadoHelper.getIdUsuarioLogado(), id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        favoritoService.remover(usuarioAutenticadoHelper.getIdUsuarioLogado(), id);
        return ResponseEntity.noContent().build();
    }
}
