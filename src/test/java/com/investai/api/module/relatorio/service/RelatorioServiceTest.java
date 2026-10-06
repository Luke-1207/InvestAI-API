package com.investai.api.module.relatorio.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.ativo.service.AcaoDetalheService;
import com.investai.api.module.ativo.service.AcaoSugestaoService;
import com.investai.api.module.ativo.service.ResumoAtivoService;
import com.investai.api.module.auth.entity.Role;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.dashboard.dto.SugestaoAtivoItemDTO;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.perfil.service.PerfilService;
import com.investai.api.module.relatorio.builder.RelatorioAtivoBuilder;
import com.investai.api.module.relatorio.dto.DadosRelatorioAtivoFixo;
import com.investai.api.module.relatorio.dto.DadosRelatorioAtivoVariavel;
import com.investai.api.module.relatorio.dto.RelatorioGeradoDTO;
import com.investai.api.module.relatorio.dto.TituloRendaFixaRelatorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    private static final byte[] PDF = {37, 80, 68, 70};

    @Mock
    private AcaoDetalheService acaoDetalheService;

    @Mock
    private AcaoSugestaoService acaoSugestaoService;

    @Mock
    private ResumoAtivoService resumoAtivoService;

    @Mock
    private PerfilService perfilService;

    @Mock
    private RelatorioAtivoBuilder relatorioAtivoBuilder;

    @Mock
    private RelatorioRendaFixaService relatorioRendaFixaService;

    @InjectMocks
    private RelatorioService relatorioService;

    @Captor
    private ArgumentCaptor<DadosRelatorioAtivoVariavel> dadosVariavelCaptor;

    @Captor
    private ArgumentCaptor<DadosRelatorioAtivoFixo> dadosFixoCaptor;

    private final Usuario usuario = Usuario.builder()
            .id(UUID.randomUUID()).nome("Lucas Silva").email("lucas@email.com").role(Role.USUARIO).build();

    private final PerfilResponseDTO perfil = PerfilResponseDTO.builder().perfilPreenchido(true).build();

    private final AcaoDetalheResponseDTO ativo = AcaoDetalheResponseDTO.builder()
            .codigo("TAEE11").nome("Transmissora Aliança").tipo(TipoAtivo.ACAO).build();

    @Test
    @DisplayName("gerarRelatorioAtivoVariavel - deve reunir perfil, ativo, compatibilidade e resumo")
    void gerarRelatorioAtivoVariavel_deveReunirTodosOsDados() {
        SugestaoAtivoItemDTO sugestao = SugestaoAtivoItemDTO.builder().score(62).build();
        when(acaoDetalheService.obterDetalhe("taee11", "1A")).thenReturn(ativo);
        when(perfilService.obterPerfil(usuario.getId())).thenReturn(perfil);
        when(acaoSugestaoService.obterSugestao("TAEE11", usuario.getId())).thenReturn(sugestao);
        when(resumoAtivoService.obterResumo(usuario.getId(), ativo)).thenReturn(Optional.of("Resumo da IA"));
        when(relatorioAtivoBuilder.construir(any(DadosRelatorioAtivoVariavel.class))).thenReturn(PDF);

        RelatorioGeradoDTO relatorio = relatorioService.gerarRelatorioAtivoVariavel("taee11", usuario);

        assertThat(relatorio.nomeArquivo()).isEqualTo("analise-TAEE11.pdf");
        assertThat(relatorio.conteudo()).isEqualTo(PDF);

        verify(relatorioAtivoBuilder).construir(dadosVariavelCaptor.capture());
        DadosRelatorioAtivoVariavel dados = dadosVariavelCaptor.getValue();
        assertThat(dados.nomeUsuario()).isEqualTo("Lucas Silva");
        assertThat(dados.geradoEm()).isNotNull();
        assertThat(dados.perfil()).isSameAs(perfil);
        assertThat(dados.ativo()).isSameAs(ativo);
        assertThat(dados.compatibilidade()).isSameAs(sugestao);
        assertThat(dados.resumoIa()).isEqualTo("Resumo da IA");
    }

    @Test
    @DisplayName("gerarRelatorioAtivoVariavel - sem resumo e sem compatibilidade deve gerar o PDF mesmo assim")
    void gerarRelatorioAtivoVariavel_semResumoNemCompatibilidade_deveGerarMesmoAssim() {
        when(acaoDetalheService.obterDetalhe("TAEE11", "1A")).thenReturn(ativo);
        when(perfilService.obterPerfil(usuario.getId())).thenReturn(perfil);
        when(acaoSugestaoService.obterSugestao("TAEE11", usuario.getId())).thenReturn(null);
        when(resumoAtivoService.obterResumo(usuario.getId(), ativo)).thenReturn(Optional.empty());
        when(relatorioAtivoBuilder.construir(any(DadosRelatorioAtivoVariavel.class))).thenReturn(PDF);

        relatorioService.gerarRelatorioAtivoVariavel("TAEE11", usuario);

        verify(relatorioAtivoBuilder).construir(dadosVariavelCaptor.capture());
        assertThat(dadosVariavelCaptor.getValue().compatibilidade()).isNull();
        assertThat(dadosVariavelCaptor.getValue().resumoIa()).isNull();
    }

    @Test
    @DisplayName("gerarRelatorioAtivoVariavel - ativo inexistente deve propagar ResourceNotFoundException")
    void gerarRelatorioAtivoVariavel_ativoInexistente_devePropagarNotFound() {
        when(acaoDetalheService.obterDetalhe("XXXX", "1A"))
                .thenThrow(new ResourceNotFoundException("Ativo não cadastrado ou inativo: XXXX"));

        assertThatThrownBy(() -> relatorioService.gerarRelatorioAtivoVariavel("XXXX", usuario))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(relatorioAtivoBuilder);
    }

    @Test
    @DisplayName("gerarRelatorioAtivoFixo - deve montar os dados e gerar nome de arquivo sem acentos nem espaços")
    void gerarRelatorioAtivoFixo_deveMontarDadosENomeDeArquivo() {
        TituloRendaFixaRelatorio titulo = TituloRendaFixaRelatorio.builder()
                .identificador("CDB-Banco Aliança S.A.").nome("CDB Banco Aliança S.A.").build();
        when(relatorioRendaFixaService.buscarTitulo("id-do-titulo")).thenReturn(titulo);
        when(perfilService.obterPerfil(usuario.getId())).thenReturn(perfil);
        when(relatorioAtivoBuilder.construir(any(DadosRelatorioAtivoFixo.class))).thenReturn(PDF);

        RelatorioGeradoDTO relatorio = relatorioService.gerarRelatorioAtivoFixo("id-do-titulo", usuario);

        assertThat(relatorio.nomeArquivo()).isEqualTo("analise-CDB-Banco-Alianca-S-A.pdf");
        assertThat(relatorio.conteudo()).isEqualTo(PDF);

        verify(relatorioAtivoBuilder).construir(dadosFixoCaptor.capture());
        assertThat(dadosFixoCaptor.getValue().nomeUsuario()).isEqualTo("Lucas Silva");
        assertThat(dadosFixoCaptor.getValue().perfil()).isSameAs(perfil);
        assertThat(dadosFixoCaptor.getValue().titulo()).isSameAs(titulo);
        verifyNoInteractions(acaoDetalheService, resumoAtivoService);
    }
}
