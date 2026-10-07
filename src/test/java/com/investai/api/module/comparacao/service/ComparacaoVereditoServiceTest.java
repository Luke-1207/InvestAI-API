package com.investai.api.module.comparacao.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.IaIndisponivelException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.infra.rabbitmq.IaMensagemPublisher;
import com.investai.api.infra.rabbitmq.dto.ComparacaoIaResponseDTO;
import com.investai.api.infra.rabbitmq.dto.PerfilIaDTO;
import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.service.AcaoDetalheService;
import com.investai.api.module.comparacao.dto.TipoAtivoComparacao;
import com.investai.api.module.comparacao.dto.VereditoComparacaoResponseDTO;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.entity.PerfilRisco;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import com.investai.api.module.rendafixa.entity.Indexador;
import com.investai.api.module.rendafixa.entity.TipoLiquidez;
import com.investai.api.module.rendafixa.entity.TipoTesouro;
import com.investai.api.module.rendafixa.entity.TipoTituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloPrivado;
import com.investai.api.module.rendafixa.entity.TituloTesouro;
import com.investai.api.module.rendafixa.repository.TituloPrivadoRepository;
import com.investai.api.module.rendafixa.repository.TituloTesouroRepository;
import com.investai.api.shared.event.PerfilAlteradoEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComparacaoVereditoServiceTest {

    private static final String TESOURO = "tesouro-selic-2029";

    @Mock
    private PerfilInvestidorRepository perfilInvestidorRepository;

    @Mock
    private AcaoDetalheService acaoDetalheService;

    @Mock
    private TituloTesouroRepository tituloTesouroRepository;

    @Mock
    private TituloPrivadoRepository tituloPrivadoRepository;

    @Mock
    private IaMensagemPublisher iaMensagemPublisher;

    @InjectMocks
    private ComparacaoVereditoService service;

    @Captor
    private ArgumentCaptor<Map<String, Object>> ativoACaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> ativoBCaptor;

    @Captor
    private ArgumentCaptor<PerfilIaDTO> perfilCaptor;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID tituloId = UUID.randomUUID();

    private PerfilInvestidor.PerfilInvestidorBuilder perfil() {
        return PerfilInvestidor.builder()
                .perfilRisco("MODERADO")
                .horizonte("LONGO_PRAZO")
                .objetivo("CRESCIMENTO_PATRIMONIO")
                .valorDisponivel(new BigDecimal("15000"))
                .tiposAceitos(List.of("ACAO"))
                .perfilPreenchido(true);
    }

    private AcaoDetalheResponseDTO.AcaoDetalheResponseDTOBuilder acao(String codigo, TipoAtivo tipo) {
        return AcaoDetalheResponseDTO.builder()
                .codigo(codigo)
                .nome("Ativo " + codigo)
                .tipo(tipo)
                .setor("Energia Elétrica")
                .cotacaoDisponivel(true)
                .preco(new BigDecimal("38.42"))
                .variacaoPercentual(new BigDecimal("1.23"))
                .dividendYield(new BigDecimal("8.4"));
    }

    private TituloTesouro.TituloTesouroBuilder tesouro() {
        return TituloTesouro.builder()
                .id(UUID.randomUUID())
                .codigo(TESOURO)
                .nome("Tesouro Selic 2029")
                .tipo(TipoTesouro.SELIC)
                .taxaAnual(new BigDecimal("0.10"))
                .precoMinimo(new BigDecimal("160"))
                .vencimento(LocalDate.of(2029, 3, 1))
                .disponivel(true);
    }

    private TituloPrivado.TituloPrivadoBuilder cdb() {
        return TituloPrivado.builder()
                .id(tituloId)
                .tipo(TipoTituloPrivado.CDB)
                .emissor("Banco Inter")
                .indexador(Indexador.CDI)
                .taxaPercentual(new BigDecimal("110"))
                .vencimento(LocalDate.of(2028, 3, 1))
                .investimentoMinimo(new BigDecimal("1000"))
                .liquidez(TipoLiquidez.DIARIA)
                .garantidoFgc(true)
                .ativo(true);
    }

    private void comPerfil() {
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil().build()));
    }

    private void comAcao(String codigo, TipoAtivo tipo) {
        when(acaoDetalheService.obterDetalhe(codigo, null)).thenReturn(acao(codigo, tipo).build());
    }

    private void iaResponde(String veredito, String erro) {
        when(iaMensagemPublisher.enviarComparacaoEAguardar(any(), any(), any()))
                .thenReturn(ComparacaoIaResponseDTO.builder().veredito(veredito).erro(erro).build());
    }

    private VereditoComparacaoResponseDTO compararAcoes() {
        return service.obterVeredito(usuarioId,
                TipoAtivoComparacao.ACAO, "TAEE11", TipoAtivoComparacao.FII, "HGLG11");
    }

    @Test
    @DisplayName("obterVeredito - deve enviar perfil e os dois ativos de renda variável à IA")
    void obterVeredito_deveCompararDoisAtivosDeRendaVariavel() {
        comPerfil();
        comAcao("TAEE11", TipoAtivo.ACAO);
        comAcao("HGLG11", TipoAtivo.FII);
        iaResponde("TAEE11 combina mais com você.", null);

        VereditoComparacaoResponseDTO resposta = compararAcoes();

        assertThat(resposta.getVeredito()).isEqualTo("TAEE11 combina mais com você.");
        assertThat(resposta.isSimplificado()).isFalse();

        verify(iaMensagemPublisher).enviarComparacaoEAguardar(
                perfilCaptor.capture(), ativoACaptor.capture(), ativoBCaptor.capture());
        assertThat(perfilCaptor.getValue().getPerfilRisco()).isEqualTo(PerfilRisco.MODERADO);
        assertThat(ativoACaptor.getValue()).containsEntry("codigo", "TAEE11").containsEntry("tipo", "ACAO");
        assertThat(ativoBCaptor.getValue()).containsEntry("codigo", "HGLG11").containsEntry("tipo", "FII");
    }

    @Test
    @DisplayName("obterVeredito - deve comparar renda variável com título do Tesouro pelo código")
    void obterVeredito_deveCompararAcaoComTesouro() {
        comPerfil();
        comAcao("BOVA11", TipoAtivo.ETF);
        when(tituloTesouroRepository.findByCodigo(TESOURO)).thenReturn(Optional.of(tesouro().build()));
        iaResponde("Veredito", null);

        service.obterVeredito(usuarioId, TipoAtivoComparacao.ETF, "BOVA11", TipoAtivoComparacao.TESOURO, TESOURO);

        verify(iaMensagemPublisher).enviarComparacaoEAguardar(any(), ativoACaptor.capture(), ativoBCaptor.capture());
        assertThat(ativoACaptor.getValue()).containsEntry("tipo", "ETF");
        assertThat(ativoBCaptor.getValue())
                .containsEntry("codigo", TESOURO)
                .containsEntry("tipo", "TESOURO")
                .containsEntry("indexador", "SELIC");
    }

    @Test
    @DisplayName("obterVeredito - deve comparar dois títulos de renda fixa, privado por id e Tesouro por código")
    void obterVeredito_deveCompararTituloPrivadoComTesouro() {
        comPerfil();
        when(tituloPrivadoRepository.findById(tituloId)).thenReturn(Optional.of(cdb().build()));
        when(tituloTesouroRepository.findByCodigo(TESOURO)).thenReturn(Optional.of(tesouro().build()));
        iaResponde("Veredito", null);

        service.obterVeredito(usuarioId,
                TipoAtivoComparacao.CDB, tituloId.toString(), TipoAtivoComparacao.TESOURO, TESOURO);

        verify(iaMensagemPublisher).enviarComparacaoEAguardar(any(), ativoACaptor.capture(), ativoBCaptor.capture());
        assertThat(ativoACaptor.getValue())
                .containsEntry("codigo", tituloId.toString())
                .containsEntry("tipo", "CDB")
                .containsEntry("garantidoFGC", true);
        assertThat(ativoBCaptor.getValue()).containsEntry("tipo", "TESOURO");
        verifyNoInteractions(acaoDetalheService);
    }

    @Test
    @DisplayName("obterVeredito - deve aceitar o id do título do Tesouro quando o código não bate")
    void obterVeredito_deveAceitarIdDoTesouro() {
        TituloTesouro titulo = tesouro().build();
        comPerfil();
        comAcao("TAEE11", TipoAtivo.ACAO);
        when(tituloTesouroRepository.findByCodigo(titulo.getId().toString())).thenReturn(Optional.empty());
        when(tituloTesouroRepository.findById(titulo.getId())).thenReturn(Optional.of(titulo));
        iaResponde("Veredito", null);

        VereditoComparacaoResponseDTO resposta = service.obterVeredito(usuarioId,
                TipoAtivoComparacao.ACAO, "TAEE11", TipoAtivoComparacao.TESOURO, titulo.getId().toString());

        assertThat(resposta.getVeredito()).isEqualTo("Veredito");
    }

    @Test
    @DisplayName("obterVeredito - deve reaproveitar o veredito em cache para o mesmo par")
    void obterVeredito_deveUsarCache() {
        comPerfil();
        comAcao("TAEE11", TipoAtivo.ACAO);
        comAcao("HGLG11", TipoAtivo.FII);
        iaResponde("Veredito", null);

        compararAcoes();
        VereditoComparacaoResponseDTO segunda = compararAcoes();

        assertThat(segunda.getVeredito()).isEqualTo("Veredito");
        verify(iaMensagemPublisher, times(1)).enviarComparacaoEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("obterVeredito - deve devolver o veredito simplificado sinalizado e sem guardar em cache")
    void obterVeredito_deveSinalizarVereditoSimplificado() {
        comPerfil();
        comAcao("TAEE11", TipoAtivo.ACAO);
        comAcao("HGLG11", TipoAtivo.FII);
        iaResponde("Veredito por regras", "Veredito simplificado, análise via IA temporariamente indisponível.");

        VereditoComparacaoResponseDTO resposta = compararAcoes();
        compararAcoes();

        assertThat(resposta.getVeredito()).isEqualTo("Veredito por regras");
        assertThat(resposta.isSimplificado()).isTrue();
        verify(iaMensagemPublisher, times(2)).enviarComparacaoEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("aoAlterarPerfil - deve descartar os vereditos em cache do usuário")
    void aoAlterarPerfil_deveInvalidarCache() {
        comPerfil();
        comAcao("TAEE11", TipoAtivo.ACAO);
        comAcao("HGLG11", TipoAtivo.FII);
        iaResponde("Veredito", null);

        compararAcoes();
        service.aoAlterarPerfil(new PerfilAlteradoEvent(this, usuarioId));
        compararAcoes();

        verify(iaMensagemPublisher, times(2)).enviarComparacaoEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("obterVeredito - deve lançar IaIndisponivelException quando a IA responde erro sem veredito")
    void obterVeredito_deveLancarQuandoIaRespondeErro() {
        comPerfil();
        comAcao("TAEE11", TipoAtivo.ACAO);
        comAcao("HGLG11", TipoAtivo.FII);
        iaResponde(null, "Payload inválido");

        assertThatThrownBy(this::compararAcoes)
                .isInstanceOf(IaIndisponivelException.class)
                .hasMessageContaining("Não foi possível gerar o veredito");
    }

    @Test
    @DisplayName("obterVeredito - deve propagar IaIndisponivelException quando a IA não responde a tempo")
    void obterVeredito_devePropagarTimeout() {
        comPerfil();
        comAcao("TAEE11", TipoAtivo.ACAO);
        comAcao("HGLG11", TipoAtivo.FII);
        when(iaMensagemPublisher.enviarComparacaoEAguardar(any(), any(), any()))
                .thenThrow(new IaIndisponivelException("Serviço de comparação por IA não respondeu a tempo"));

        assertThatThrownBy(this::compararAcoes).isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    @DisplayName("obterVeredito - deve recusar quando o perfil ainda não foi preenchido")
    void obterVeredito_deveRecusarSemPerfilPreenchido() {
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(perfil().perfilPreenchido(false).build()));

        assertThatThrownBy(this::compararAcoes)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Preencha seu perfil");

        verifyNoInteractions(iaMensagemPublisher, acaoDetalheService);
    }

    @Test
    @DisplayName("obterVeredito - deve recusar quando o perfil não tem valor disponível")
    void obterVeredito_deveRecusarPerfilSemValorDisponivel() {
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(perfil().valorDisponivel(BigDecimal.ZERO).build()));

        assertThatThrownBy(this::compararAcoes).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("obterVeredito - deve recusar comparar um ativo com ele mesmo")
    void obterVeredito_deveRecusarAtivosIguais() {
        assertThatThrownBy(() -> service.obterVeredito(usuarioId,
                TipoAtivoComparacao.ACAO, "taee11", TipoAtivoComparacao.ACAO, " TAEE11 "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dois ativos diferentes");

        verifyNoInteractions(perfilInvestidorRepository, iaMensagemPublisher);
    }

    @Test
    @DisplayName("obterVeredito - deve recusar quando a cotação do ativo está indisponível")
    void obterVeredito_deveRecusarAtivoSemCotacao() {
        comPerfil();
        when(acaoDetalheService.obterDetalhe("TAEE11", null))
                .thenReturn(acao("TAEE11", TipoAtivo.ACAO).cotacaoDisponivel(false).preco(null).build());

        assertThatThrownBy(this::compararAcoes)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cotação de TAEE11 indisponível");

        verify(iaMensagemPublisher, never()).enviarComparacaoEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("obterVeredito - deve lançar ResourceNotFoundException para título do Tesouro inexistente ou indisponível")
    void obterVeredito_deveLancarQuandoTesouroNaoExiste() {
        comPerfil();
        comAcao("TAEE11", TipoAtivo.ACAO);
        when(tituloTesouroRepository.findByCodigo(TESOURO))
                .thenReturn(Optional.of(tesouro().disponivel(false).build()));

        assertThatThrownBy(() -> service.obterVeredito(usuarioId,
                TipoAtivoComparacao.ACAO, "TAEE11", TipoAtivoComparacao.TESOURO, TESOURO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("obterVeredito - deve lançar ResourceNotFoundException para título privado com id inválido ou inativo")
    void obterVeredito_deveLancarQuandoTituloPrivadoNaoExiste() {
        comPerfil();
        comAcao("TAEE11", TipoAtivo.ACAO);
        when(tituloPrivadoRepository.findById(tituloId)).thenReturn(Optional.of(cdb().ativo(false).build()));

        assertThatThrownBy(() -> service.obterVeredito(usuarioId,
                TipoAtivoComparacao.ACAO, "TAEE11", TipoAtivoComparacao.CDB, "nao-e-uuid"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.obterVeredito(usuarioId,
                TipoAtivoComparacao.ACAO, "TAEE11", TipoAtivoComparacao.LCA, tituloId.toString()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
