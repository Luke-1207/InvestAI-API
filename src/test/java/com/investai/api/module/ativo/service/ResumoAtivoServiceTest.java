package com.investai.api.module.ativo.service;

import com.investai.api.infra.exception.IaIndisponivelException;
import com.investai.api.infra.rabbitmq.IaMensagemPublisher;
import com.investai.api.infra.rabbitmq.dto.ModuloIa;
import com.investai.api.infra.rabbitmq.dto.PerfilIaDTO;
import com.investai.api.infra.rabbitmq.dto.ResumoResponseDTO;
import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.perfil.entity.PerfilInvestidor;
import com.investai.api.module.perfil.entity.PerfilRisco;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumoAtivoServiceTest {

    @Mock
    private PerfilInvestidorRepository perfilInvestidorRepository;

    @Mock
    private IaMensagemPublisher iaMensagemPublisher;

    @InjectMocks
    private ResumoAtivoService resumoAtivoService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> ativoCaptor;

    @Captor
    private ArgumentCaptor<PerfilIaDTO> perfilCaptor;

    private final UUID usuarioId = UUID.randomUUID();

    private PerfilInvestidor perfilPreenchido() {
        return PerfilInvestidor.builder()
                .perfilRisco("MODERADO")
                .horizonte("LONGO_PRAZO")
                .objetivo("CRESCIMENTO_PATRIMONIO")
                .valorDisponivel(new BigDecimal("15000"))
                .tiposAceitos(List.of("ACAO"))
                .perfilPreenchido(true)
                .build();
    }

    private AcaoDetalheResponseDTO.AcaoDetalheResponseDTOBuilder ativo() {
        return AcaoDetalheResponseDTO.builder()
                .codigo("TAEE11")
                .nome("Transmissora Aliança")
                .tipo(TipoAtivo.ACAO)
                .setor("Energia Elétrica")
                .cotacaoDisponivel(true)
                .preco(new BigDecimal("38.42"))
                .variacaoPercentual(new BigDecimal("1.23"))
                .dividendYield(new BigDecimal("8.4"))
                .precoValorPatrimonial(new BigDecimal("1.35"))
                .minimo52Semanas(new BigDecimal("31.2"))
                .maximo52Semanas(new BigDecimal("41.9"));
    }

    private void iaResponde(String resumo, String erro) {
        when(iaMensagemPublisher.enviarResumoEAguardar(eq(ModuloIa.VARIAVEL), any(), any()))
                .thenReturn(ResumoResponseDTO.builder().resumo(resumo).erro(erro).build());
    }

    @Test
    @DisplayName("obterResumo - deve pedir o resumo à IA com perfil e dados do ativo")
    void obterResumo_devePedirResumoAIa() {
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilPreenchido()));
        iaResponde("Resumo da IA", null);

        Optional<String> resultado = resumoAtivoService.obterResumo(usuarioId, ativo().build());

        assertThat(resultado).contains("Resumo da IA");
        verify(iaMensagemPublisher).enviarResumoEAguardar(eq(ModuloIa.VARIAVEL), perfilCaptor.capture(), ativoCaptor.capture());
        assertThat(perfilCaptor.getValue().getPerfilRisco()).isEqualTo(PerfilRisco.MODERADO);
        assertThat(ativoCaptor.getValue())
                .containsEntry("codigo", "TAEE11")
                .containsEntry("tipo", "ACAO")
                .containsEntry("preco", new BigDecimal("38.42"))
                .containsEntry("dy", new BigDecimal("8.4"))
                .containsEntry("variacao30d", new BigDecimal("1.23"))
                .containsEntry("pvp", new BigDecimal("1.35"))
                .containsEntry("variacao52s", Map.of("min", new BigDecimal("31.2"), "max", new BigDecimal("41.9")))
                .doesNotContainKey("pl");
    }

    @Test
    @DisplayName("obterResumo - segunda chamada do mesmo usuário e ativo deve vir do cache")
    void obterResumo_segundaChamada_deveUsarCache() {
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilPreenchido()));
        iaResponde("Resumo da IA", null);

        resumoAtivoService.obterResumo(usuarioId, ativo().build());
        Optional<String> segunda = resumoAtivoService.obterResumo(usuarioId, ativo().codigo("taee11").build());

        assertThat(segunda).contains("Resumo da IA");
        verify(iaMensagemPublisher, times(1)).enviarResumoEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("obterResumo - cache é por usuário, outro usuário deve gerar nova chamada")
    void obterResumo_outroUsuario_deveChamarIaDeNovo() {
        UUID outroUsuario = UUID.randomUUID();
        when(perfilInvestidorRepository.findByUsuarioId(any())).thenReturn(Optional.of(perfilPreenchido()));
        iaResponde("Resumo da IA", null);

        resumoAtivoService.obterResumo(usuarioId, ativo().build());
        resumoAtivoService.obterResumo(outroUsuario, ativo().build());

        verify(iaMensagemPublisher, times(2)).enviarResumoEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("aoAlterarPerfil - deve invalidar só os resumos do usuário que alterou o perfil")
    void aoAlterarPerfil_deveInvalidarResumosDoUsuario() {
        UUID outroUsuario = UUID.randomUUID();
        when(perfilInvestidorRepository.findByUsuarioId(any())).thenReturn(Optional.of(perfilPreenchido()));
        iaResponde("Resumo da IA", null);
        resumoAtivoService.obterResumo(usuarioId, ativo().build());
        resumoAtivoService.obterResumo(outroUsuario, ativo().build());

        resumoAtivoService.aoAlterarPerfil(new PerfilAlteradoEvent(this, usuarioId));
        resumoAtivoService.obterResumo(usuarioId, ativo().build());
        resumoAtivoService.obterResumo(outroUsuario, ativo().build());

        verify(iaMensagemPublisher, times(3)).enviarResumoEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("obterResumo - IA indisponível deve devolver vazio sem propagar a exceção, e não cachear")
    void obterResumo_iaIndisponivel_deveDevolverVazio() {
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilPreenchido()));
        when(iaMensagemPublisher.enviarResumoEAguardar(any(), any(), any()))
                .thenThrow(new IaIndisponivelException("timeout"));

        assertThat(resumoAtivoService.obterResumo(usuarioId, ativo().build())).isEmpty();
        assertThat(resumoAtivoService.obterResumo(usuarioId, ativo().build())).isEmpty();
        verify(iaMensagemPublisher, times(2)).enviarResumoEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("obterResumo - resposta com erro ou resumo em branco deve devolver vazio")
    void obterResumo_respostaComErroOuEmBranco_deveDevolverVazio() {
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilPreenchido()));

        iaResponde(null, "Payload inválido");
        assertThat(resumoAtivoService.obterResumo(usuarioId, ativo().build())).isEmpty();

        iaResponde("   ", null);
        assertThat(resumoAtivoService.obterResumo(usuarioId, ativo().build())).isEmpty();
    }

    @Test
    @DisplayName("obterResumo - perfil não preenchido não deve chamar a IA")
    void obterResumo_perfilNaoPreenchido_naoDeveChamarIa() {
        PerfilInvestidor perfil = perfilPreenchido();
        perfil.setPerfilPreenchido(false);
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));

        assertThat(resumoAtivoService.obterResumo(usuarioId, ativo().build())).isEmpty();
        verify(iaMensagemPublisher, never()).enviarResumoEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("obterResumo - ativo sem cotação não deve chamar a IA")
    void obterResumo_ativoSemCotacao_naoDeveChamarIa() {
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilPreenchido()));

        Optional<String> resultado = resumoAtivoService.obterResumo(
                usuarioId, ativo().cotacaoDisponivel(false).preco(null).build());

        assertThat(resultado).isEmpty();
        verify(iaMensagemPublisher, never()).enviarResumoEAguardar(any(), any(), any());
    }

    @Test
    @DisplayName("obterResumo - DY e variação ausentes devem ir como zero para a IA")
    void obterResumo_dyEVariacaoAusentes_devemIrComoZero() {
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilPreenchido()));
        iaResponde("Resumo da IA", null);

        resumoAtivoService.obterResumo(usuarioId, ativo()
                .dividendYield(null).variacaoPercentual(null).precoValorPatrimonial(null)
                .minimo52Semanas(null).build());

        verify(iaMensagemPublisher).enviarResumoEAguardar(any(), any(), ativoCaptor.capture());
        assertThat(ativoCaptor.getValue())
                .containsEntry("dy", BigDecimal.ZERO)
                .containsEntry("variacao30d", BigDecimal.ZERO)
                .doesNotContainKeys("pvp", "variacao52s");
    }
}
