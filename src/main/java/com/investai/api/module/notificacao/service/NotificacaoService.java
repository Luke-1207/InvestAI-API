package com.investai.api.module.notificacao.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.module.favorito.dto.AtivoFavoritavelDTO;
import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.favorito.service.AtivoFavoritavelService;
import com.investai.api.module.notificacao.dto.ComunicadoSistemaRequestDTO;
import com.investai.api.module.notificacao.dto.ComunicadoSistemaResponseDTO;
import com.investai.api.module.notificacao.dto.ContagemNaoLidasResponseDTO;
import com.investai.api.module.notificacao.dto.MarcarTodasLidasResponseDTO;
import com.investai.api.module.notificacao.dto.NotificacaoResponseDTO;
import com.investai.api.module.notificacao.entity.Notificacao;
import com.investai.api.module.notificacao.entity.TipoNotificacao;
import com.investai.api.module.notificacao.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    static final int TAMANHO_MAXIMO_PAGINA = 50;

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtivoFavoritavelService ativoFavoritavelService;

    @Transactional(readOnly = true)
    public Page<NotificacaoResponseDTO> listar(UUID usuarioId, Boolean lida, int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_PAGINA));
        Page<Notificacao> resultado = lida == null
                ? notificacaoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId, pageRequest)
                : notificacaoRepository.findByUsuarioIdAndLidaOrderByCriadoEmDesc(usuarioId, lida, pageRequest);

        Map<TipoAtivoFavorito, Map<UUID, AtivoFavoritavelDTO>> ativos = new EnumMap<>(TipoAtivoFavorito.class);
        resultado.getContent().stream()
                .filter(n -> n.getTipoAtivo() != null)
                .collect(Collectors.groupingBy(Notificacao::getTipoAtivo, Collectors.mapping(Notificacao::getAtivoId, Collectors.toSet())))
                .forEach((tipo, ids) -> ativos.put(tipo, ativoFavoritavelService.buscarPorIds(tipo, ids)));

        return resultado.map(n -> toResponse(n, ativos));
    }

    @Transactional(readOnly = true)
    public ContagemNaoLidasResponseDTO contarNaoLidas(UUID usuarioId) {
        return new ContagemNaoLidasResponseDTO(notificacaoRepository.countByUsuarioIdAndLidaFalse(usuarioId));
    }

    @Transactional
    public NotificacaoResponseDTO marcarComoLida(UUID usuarioId, UUID notificacaoId) {
        Notificacao notificacao = notificacaoRepository.findByIdAndUsuarioId(notificacaoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));
        notificacao.marcarComoLida(LocalDateTime.now());
        return toResponse(notificacaoRepository.save(notificacao), Map.of());
    }

    @Transactional
    public MarcarTodasLidasResponseDTO marcarTodasComoLidas(UUID usuarioId) {
        return new MarcarTodasLidasResponseDTO(notificacaoRepository.marcarTodasComoLidas(usuarioId, LocalDateTime.now()));
    }

    @Transactional
    public ComunicadoSistemaResponseDTO enviarComunicado(ComunicadoSistemaRequestDTO dto) {
        String titulo = dto.getTitulo().trim();
        String mensagem = dto.getMensagem().trim();
        List<UUID> destinatarios = usuarioRepository.findIdsAtivos();
        List<Notificacao> notificacoes = destinatarios.stream()
                .map(id -> Notificacao.builder()
                        .usuarioId(id)
                        .tipo(TipoNotificacao.SISTEMA)
                        .titulo(titulo)
                        .mensagem(mensagem)
                        .build())
                .toList();
        notificacaoRepository.saveAll(notificacoes);
        return new ComunicadoSistemaResponseDTO(notificacoes.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notificacao criar(UUID usuarioId, TipoNotificacao tipo, String titulo, String mensagem,
                             TipoAtivoFavorito tipoAtivo, UUID ativoId) {
        String tituloAjustado = titulo.length() > Notificacao.TAMANHO_MAXIMO_TITULO
                ? titulo.substring(0, Notificacao.TAMANHO_MAXIMO_TITULO)
                : titulo;
        return notificacaoRepository.save(Notificacao.builder()
                .usuarioId(usuarioId)
                .tipo(tipo)
                .titulo(tituloAjustado)
                .mensagem(mensagem)
                .tipoAtivo(tipoAtivo)
                .ativoId(ativoId)
                .build());
    }

    @Transactional(readOnly = true)
    public boolean jaNotificadoDesde(UUID usuarioId, TipoNotificacao tipo, UUID ativoId, LocalDateTime desde) {
        return notificacaoRepository.existsByUsuarioIdAndTipoAndAtivoIdAndCriadoEmAfter(usuarioId, tipo, ativoId, desde);
    }

    private NotificacaoResponseDTO toResponse(Notificacao n, Map<TipoAtivoFavorito, Map<UUID, AtivoFavoritavelDTO>> ativos) {
        String codigo = n.getTipoAtivo() == null ? null
                : ativos.getOrDefault(n.getTipoAtivo(), Map.of()).values().stream()
                .filter(a -> Objects.equals(a.id(), n.getAtivoId()))
                .map(AtivoFavoritavelDTO::codigo)
                .findFirst()
                .orElse(null);
        return NotificacaoResponseDTO.builder()
                .id(n.getId())
                .tipo(n.getTipo())
                .titulo(n.getTitulo())
                .mensagem(n.getMensagem())
                .tipoAtivo(n.getTipoAtivo())
                .ativoId(n.getAtivoId())
                .codigoAtivo(codigo)
                .lida(n.isLida())
                .lidaEm(n.getLidaEm())
                .criadoEm(n.getCriadoEm())
                .build();
    }
}
