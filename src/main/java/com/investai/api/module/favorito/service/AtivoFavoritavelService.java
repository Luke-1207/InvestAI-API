package com.investai.api.module.favorito.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.favorito.dto.AtivoFavoritavelDTO;
import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.notificacao.entity.TipoNotificacao;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AtivoFavoritavelService {

    private static final List<TipoNotificacao> ALERTAS_RENDA_VARIAVEL =
            List.of(TipoNotificacao.VARIACAO_ALTA, TipoNotificacao.VARIACAO_BAIXA);
    private static final List<TipoNotificacao> ALERTAS_RENDA_FIXA =
            List.of(TipoNotificacao.TITULO_VENCENDO);

    private final AcaoRepository acaoRepository;
    private final TituloTesouroRepository tituloTesouroRepository;
    private final TituloPrivadoRepository tituloPrivadoRepository;

    public static List<TipoNotificacao> tiposAlertaDisponiveis(TipoAtivoFavorito tipo) {
        return tipo == TipoAtivoFavorito.ACAO_FII_ETF ? ALERTAS_RENDA_VARIAVEL : ALERTAS_RENDA_FIXA;
    }

    public AtivoFavoritavelDTO resolver(TipoAtivoFavorito tipo, String codigo, UUID ativoId) {
        boolean temCodigo = codigo != null && !codigo.isBlank();
        if (!temCodigo && ativoId == null) {
            throw new BusinessException("Informe o código ou o identificador do ativo");
        }
        Optional<AtivoFavoritavelDTO> encontrado = switch (tipo) {
            case ACAO_FII_ETF -> (ativoId != null
                    ? acaoRepository.findById(ativoId).filter(Acao::isAtivo)
                    : acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue(codigo.trim()))
                    .map(this::deAcao);
            case TITULO_TESOURO -> (ativoId != null
                    ? tituloTesouroRepository.findById(ativoId)
                    : tituloTesouroRepository.findByCodigo(codigo.trim()))
                    .map(this::deTesouro);
            case TITULO_PRIVADO -> Optional.ofNullable(ativoId)
                    .flatMap(tituloPrivadoRepository::findById)
                    .filter(TituloPrivado::isAtivo)
                    .map(this::dePrivado);
        };
        return encontrado.orElseThrow(() -> new ResourceNotFoundException("Ativo não encontrado para favoritar"));
    }

    public Map<UUID, AtivoFavoritavelDTO> buscarPorIds(TipoAtivoFavorito tipo, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<AtivoFavoritavelDTO> ativos = switch (tipo) {
            case ACAO_FII_ETF -> acaoRepository.findAllById(ids).stream().map(this::deAcao).toList();
            case TITULO_TESOURO -> tituloTesouroRepository.findAllById(ids).stream().map(this::deTesouro).toList();
            case TITULO_PRIVADO -> tituloPrivadoRepository.findAllById(ids).stream().map(this::dePrivado).toList();
        };
        return ativos.stream().collect(Collectors.toMap(AtivoFavoritavelDTO::id, Function.identity()));
    }

    private AtivoFavoritavelDTO deAcao(Acao acao) {
        return new AtivoFavoritavelDTO(acao.getId(), TipoAtivoFavorito.ACAO_FII_ETF, acao.getCodigo(), acao.getNome(), null);
    }

    private AtivoFavoritavelDTO deTesouro(TituloTesouro titulo) {
        return new AtivoFavoritavelDTO(titulo.getId(), TipoAtivoFavorito.TITULO_TESOURO, titulo.getCodigo(), titulo.getNome(), titulo.getVencimento());
    }

    private AtivoFavoritavelDTO dePrivado(TituloPrivado titulo) {
        String nome = titulo.getTipo().name() + " " + titulo.getEmissor();
        return new AtivoFavoritavelDTO(titulo.getId(), TipoAtivoFavorito.TITULO_PRIVADO, null, nome, titulo.getVencimento());
    }
}
