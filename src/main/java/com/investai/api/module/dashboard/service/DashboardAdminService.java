package com.investai.api.module.dashboard.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.investai.api.infra.ia.IaHealthClient;
import com.investai.api.infra.ia.dto.IaHealthStatusDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.auth.repository.UsuarioRepository;
import com.investai.api.module.dashboard.dto.DashboardAdminResponseDTO;
import com.investai.api.module.dashboard.dto.NivelUrgencia;
import com.investai.api.module.dashboard.dto.StatusIaResponseDTO;
import com.investai.api.module.dashboard.dto.TituloVencendoDTO;
import com.investai.api.module.perfil.entity.PerfilRisco;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DashboardAdminService {

    private static final String CHAVE_CACHE = "admin";
    private static final int JANELA_DIAS = 30;

    private final UsuarioRepository usuarioRepository;
    private final PerfilInvestidorRepository perfilInvestidorRepository;
    private final AcaoRepository acaoRepository;
    private final TituloTesouroRepository tituloTesouroRepository;
    private final TituloPrivadoRepository tituloPrivadoRepository;
    private final IaHealthClient iaHealthClient;

    private final Cache<String, DashboardAdminResponseDTO> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1)
            .build();

    public DashboardAdminResponseDTO obterMetricasAdmin() {
        return cache.get(CHAVE_CACHE, chave -> montarMetricas());
    }

    public StatusIaResponseDTO obterStatusIa() {
        IaHealthStatusDTO status = iaHealthClient.verificarStatus();
        return StatusIaResponseDTO.builder()
                .disponivel(status.isDisponivel())
                .rabbitmqConectado(status.getRabbitmqConectado())
                .verificadoEm(LocalDateTime.now())
                .build();
    }

    private DashboardAdminResponseDTO montarMetricas() {
        Map<String, Long> distribuicaoRisco = new LinkedHashMap<>();
        for (PerfilRisco risco : PerfilRisco.values()) {
            distribuicaoRisco.put(risco.name(), perfilInvestidorRepository.countByPerfilPreenchidoTrueAndPerfilRisco(risco.name()));
        }

        Map<String, Long> distribuicaoAtivos = new LinkedHashMap<>();
        for (TipoAtivo tipo : TipoAtivo.values()) {
            distribuicaoAtivos.put(tipo.name(), acaoRepository.countByAtivoTrueAndTipo(tipo));
        }
        for (TipoTituloPrivado tipo : TipoTituloPrivado.values()) {
            distribuicaoAtivos.put(tipo.name(), tituloPrivadoRepository.countByAtivoTrueAndTipo(tipo));
        }
        distribuicaoAtivos.put("TESOURO", tituloTesouroRepository.countByDisponivelTrue());

        LocalDateTime ultimaSincronizacaoTesouro = tituloTesouroRepository.findTopByOrderBySincronizadoEmDesc()
                .map(TituloTesouro::getSincronizadoEm)
                .orElse(null);

        IaHealthStatusDTO statusIa = iaHealthClient.verificarStatus();

        LocalDate hoje = LocalDate.now();
        List<TituloVencendoDTO> titulosVencendo = tituloPrivadoRepository
                .findByAtivoTrueAndVencimentoBetweenOrderByVencimentoAsc(hoje, hoje.plusDays(JANELA_DIAS))
                .stream()
                .map(titulo -> toTituloVencendoDTO(titulo, hoje))
                .toList();

        return DashboardAdminResponseDTO.builder()
                .totalUsuarios(usuarioRepository.countByDeletadoEmIsNull())
                .novosUsuariosUltimos30Dias(usuarioRepository.countByDeletadoEmIsNullAndCriadoEmGreaterThanEqual(
                        LocalDateTime.now().minusDays(JANELA_DIAS)))
                .usuariosComPerfilPreenchido(perfilInvestidorRepository.countByPerfilPreenchidoTrue())
                .distribuicaoRisco(distribuicaoRisco)
                .distribuicaoAtivosPorCategoria(distribuicaoAtivos)
                .titulosVencendoEm30Dias(titulosVencendo.size())
                .titulosVencendo(titulosVencendo)
                .ultimaSincronizacaoTesouro(ultimaSincronizacaoTesouro)
                .iaDisponivel(statusIa.isDisponivel())
                .iaRabbitmqConectado(statusIa.getRabbitmqConectado())
                .geradoEm(LocalDateTime.now())
                .build();
    }

    private TituloVencendoDTO toTituloVencendoDTO(TituloPrivado titulo, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(hoje, titulo.getVencimento());
        return TituloVencendoDTO.builder()
                .id(titulo.getId())
                .tipo(titulo.getTipo())
                .emissor(titulo.getEmissor())
                .vencimento(titulo.getVencimento())
                .diasParaVencimento(dias)
                .urgencia(NivelUrgencia.porDiasRestantes(dias))
                .build();
    }
}