package com.investai.api.module.favorito.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.entity.Acao;
import com.investai.api.module.ativo.repository.AcaoRepository;
import com.investai.api.module.favorito.dto.AtivoFavoritavelDTO;
import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtivoFavoritavelServiceTest {

    @Mock
    private AcaoRepository acaoRepository;

    @Mock
    private TituloTesouroRepository tituloTesouroRepository;

    @Mock
    private TituloPrivadoRepository tituloPrivadoRepository;

    @InjectMocks
    private AtivoFavoritavelService service;

    @Test
    @DisplayName("resolver - deve encontrar ação ativa pelo código")
    void resolver_deveEncontrarAcaoPorCodigo() {
        Acao acao = new Acao();
        acao.setId(UUID.randomUUID());
        acao.setCodigo("ITUB4");
        acao.setNome("Itaú Unibanco PN");
        when(acaoRepository.findByCodigoIgnoreCaseAndAtivoTrue("itub4")).thenReturn(Optional.of(acao));

        AtivoFavoritavelDTO dto = service.resolver(TipoAtivoFavorito.ACAO_FII_ETF, " itub4 ", null);

        assertThat(dto.id()).isEqualTo(acao.getId());
        assertThat(dto.codigo()).isEqualTo("ITUB4");
    }

    @Test
    @DisplayName("resolver - deve encontrar título do Tesouro pelo código exatamente como na rota")
    void resolver_deveEncontrarTesouroPorCodigo() {
        TituloTesouro titulo = new TituloTesouro();
        titulo.setId(UUID.randomUUID());
        titulo.setCodigo("tesouro-prefixado-2027-mock");
        titulo.setNome("Tesouro Prefixado 2027");
        titulo.setVencimento(LocalDate.of(2027, 1, 1));
        when(tituloTesouroRepository.findByCodigo("tesouro-prefixado-2027-mock")).thenReturn(Optional.of(titulo));

        AtivoFavoritavelDTO dto = service.resolver(TipoAtivoFavorito.TITULO_TESOURO, " tesouro-prefixado-2027-mock ", null);

        assertThat(dto.codigo()).isEqualTo("tesouro-prefixado-2027-mock");
        assertThat(dto.vencimento()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("resolver - título privado inativo não pode ser favoritado")
    void resolver_deveIgnorarTituloPrivadoInativo() {
        UUID id = UUID.randomUUID();
        TituloPrivado titulo = new TituloPrivado();
        titulo.setId(id);
        titulo.setAtivo(false);
        when(tituloPrivadoRepository.findById(id)).thenReturn(Optional.of(titulo));

        assertThatThrownBy(() -> service.resolver(TipoAtivoFavorito.TITULO_PRIVADO, null, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("resolver - deve exigir código ou identificador")
    void resolver_deveExigirIdentificacao() {
        assertThatThrownBy(() -> service.resolver(TipoAtivoFavorito.ACAO_FII_ETF, " ", null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("buscarPorIds - deve montar o nome do título privado com tipo e emissor")
    void buscarPorIds_deveMontarNomeDoPrivado() {
        UUID id = UUID.randomUUID();
        TituloPrivado titulo = new TituloPrivado();
        titulo.setId(id);
        titulo.setTipo(TipoTituloPrivado.values()[0]);
        titulo.setEmissor("Banco Exemplo");
        when(tituloPrivadoRepository.findAllById(List.of(id))).thenReturn(List.of(titulo));

        Map<UUID, AtivoFavoritavelDTO> mapa = service.buscarPorIds(TipoAtivoFavorito.TITULO_PRIVADO, List.of(id));

        assertThat(mapa.get(id).nome()).isEqualTo(TipoTituloPrivado.values()[0].name() + " Banco Exemplo");
    }
}
