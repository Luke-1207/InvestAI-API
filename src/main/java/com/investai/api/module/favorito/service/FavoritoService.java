package com.investai.api.module.favorito.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ConflictException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.favorito.dto.AdicionarFavoritoRequestDTO;
import com.investai.api.module.favorito.dto.AtivoFavoritavelDTO;
import com.investai.api.module.favorito.dto.ConfigurarAlertasRequestDTO;
import com.investai.api.module.favorito.dto.FavoritoResponseDTO;
import com.investai.api.module.favorito.dto.FavoritoStatusResponseDTO;
import com.investai.api.module.favorito.entity.Favorito;
import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.favorito.repository.FavoritoRepository;
import com.investai.api.module.notificacao.entity.TipoNotificacao;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final AtivoFavoritavelService ativoFavoritavelService;

    @Value("${favoritos.limite-por-usuario:50}")
    private int limitePorUsuario;

    @Transactional
    public FavoritoResponseDTO adicionar(UUID usuarioId, AdicionarFavoritoRequestDTO dto) {
        AtivoFavoritavelDTO ativo = ativoFavoritavelService.resolver(dto.getTipoAtivo(), dto.getCodigo(), dto.getAtivoId());

        if (favoritoRepository.existsByUsuarioIdAndAtivoIdAndTipoAtivo(usuarioId, ativo.id(), ativo.tipo())) {
            throw new ConflictException("Este ativo já está nos seus favoritos");
        }
        if (favoritoRepository.countByUsuarioId(usuarioId) >= limitePorUsuario) {
            throw new BusinessException("Você atingiu o limite de " + limitePorUsuario + " favoritos. Remova algum para adicionar outro.");
        }

        Favorito favorito = Favorito.builder()
                .usuarioId(usuarioId)
                .ativoId(ativo.id())
                .tipoAtivo(ativo.tipo())
                .build();
        try {
            favorito = favoritoRepository.saveAndFlush(favorito);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Este ativo já está nos seus favoritos");
        }
        return toResponse(favorito, ativo);
    }

    @Transactional(readOnly = true)
    public List<FavoritoResponseDTO> listar(UUID usuarioId) {
        List<Favorito> favoritos = favoritoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId);
        Map<TipoAtivoFavorito, Map<UUID, AtivoFavoritavelDTO>> ativosPorTipo = new EnumMap<>(TipoAtivoFavorito.class);
        favoritos.stream()
                .collect(Collectors.groupingBy(Favorito::getTipoAtivo, Collectors.mapping(Favorito::getAtivoId, Collectors.toSet())))
                .forEach((tipo, ids) -> ativosPorTipo.put(tipo, ativoFavoritavelService.buscarPorIds(tipo, ids)));

        List<FavoritoResponseDTO> resposta = new ArrayList<>();
        for (Favorito favorito : favoritos) {
            AtivoFavoritavelDTO ativo = ativosPorTipo.getOrDefault(favorito.getTipoAtivo(), Map.of()).get(favorito.getAtivoId());
            if (ativo != null) {
                resposta.add(toResponse(favorito, ativo));
            }
        }
        return resposta;
    }

    @Transactional(readOnly = true)
    public FavoritoStatusResponseDTO status(UUID usuarioId, TipoAtivoFavorito tipo, String codigo, UUID ativoId) {
        AtivoFavoritavelDTO ativo = ativoFavoritavelService.resolver(tipo, codigo, ativoId);
        return favoritoRepository.findByUsuarioIdAndAtivoIdAndTipoAtivo(usuarioId, ativo.id(), ativo.tipo())
                .map(f -> FavoritoStatusResponseDTO.builder().favoritado(true).favoritoId(f.getId()).build())
                .orElse(FavoritoStatusResponseDTO.builder().favoritado(false).build());
    }

    @Transactional
    public void remover(UUID usuarioId, UUID favoritoId) {
        favoritoRepository.delete(buscarDoUsuario(usuarioId, favoritoId));
    }

    @Transactional
    public FavoritoResponseDTO configurarAlertas(UUID usuarioId, UUID favoritoId, ConfigurarAlertasRequestDTO dto) {
        Favorito favorito = buscarDoUsuario(usuarioId, favoritoId);
        List<TipoNotificacao> disponiveis = AtivoFavoritavelService.tiposAlertaDisponiveis(favorito.getTipoAtivo());

        List<TipoNotificacao> tipos = dto.getTiposAlertaAtivos();
        if (tipos != null) {
            tipos = tipos.stream().distinct().toList();
            List<TipoNotificacao> invalidos = tipos.stream().filter(t -> !disponiveis.contains(t)).toList();
            if (!invalidos.isEmpty()) {
                throw new BusinessException("Tipo de alerta não disponível para este ativo: " + invalidos);
            }
        }

        favorito.setAlertasAtivos(dto.getAlertasAtivos());
        favorito.setTiposAlertaAtivos(tipos == null ? null : new ArrayList<>(tipos));
        if (dto.getLimiarVariacao() != null) {
            favorito.setLimiarVariacao(dto.getLimiarVariacao());
        }
        favorito = favoritoRepository.save(favorito);

        AtivoFavoritavelDTO ativo = ativoFavoritavelService.buscarPorIds(favorito.getTipoAtivo(), List.of(favorito.getAtivoId()))
                .get(favorito.getAtivoId());
        return toResponse(favorito, ativo);
    }

    private Favorito buscarDoUsuario(UUID usuarioId, UUID favoritoId) {
        return favoritoRepository.findById(favoritoId)
                .filter(f -> f.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Favorito não encontrado"));
    }

    private FavoritoResponseDTO toResponse(Favorito favorito, AtivoFavoritavelDTO ativo) {
        return FavoritoResponseDTO.builder()
                .id(favorito.getId())
                .tipoAtivo(favorito.getTipoAtivo())
                .ativoId(favorito.getAtivoId())
                .codigo(ativo != null ? ativo.codigo() : null)
                .nome(ativo != null ? ativo.nome() : null)
                .alertasAtivos(favorito.isAlertasAtivos())
                .tiposAlertaAtivos(favorito.getTiposAlertaAtivos())
                .tiposAlertaDisponiveis(AtivoFavoritavelService.tiposAlertaDisponiveis(favorito.getTipoAtivo()))
                .limiarVariacao(favorito.getLimiarVariacao())
                .criadoEm(favorito.getCriadoEm())
                .build();
    }
}
