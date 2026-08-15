package com.investai.api.module.perfil.service;

import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.ativo.entity.TipoAtivo;
import com.investai.api.module.perfil.dto.EditarPerfilRequestDTO;
import com.investai.api.module.perfil.dto.PerfilResponseDTO;
import com.investai.api.module.perfil.dto.SetorPreferidoRequestDTO;
import com.investai.api.module.perfil.entity.*;
import com.investai.api.module.perfil.repository.PerfilInvestidorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    @Mock
    private PerfilInvestidorRepository perfilInvestidorRepository;

    @Mock
    private ResumoPerfilService resumoPerfilService;

    @InjectMocks
    private PerfilService perfilService;

    private PerfilInvestidor perfilCompletoMock(UUID id) {
        return PerfilInvestidor.builder()
                .id(id)
                .perfilRisco("ARROJADO")
                .horizonte("CURTO_PRAZO")
                .objetivo("PRESERVAR_CAPITAL")
                .valorDisponivel(new BigDecimal("3500.50"))
                .tiposAceitos(List.of("ACAO", "ETF"))
                .setoresPreferidos(List.of(
                        SetorPreferido.builder().setor("Tecnologia").preferencia(PreferenciaSetor.EVITAR).build()
                ))
                .perfilPreenchido(true)
                .build();
    }

    @Test
    @DisplayName("obterPerfil - deve retornar perfil completo com resumoIA")
    void obterPerfil_deveRetornarPerfilCompleto() {
        UUID usuarioId = UUID.randomUUID();
        PerfilInvestidor perfil = perfilCompletoMock(UUID.randomUUID());

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(resumoPerfilService.gerarResumoIA(perfil)).thenReturn("Você busca preservar o capital no curto prazo, com perfil arrojado.");

        PerfilResponseDTO response = perfilService.obterPerfil(usuarioId);

        assertThat(response.getPerfilRisco().getValor()).isEqualTo("ARROJADO");
        assertThat(response.getObjetivoFinanceiro().getValor()).isEqualTo("PRESERVAR_CAPITAL");
        assertThat(response.getHorizonteInvestimento().getValor()).isEqualTo("CURTO_PRAZO");
        assertThat(response.getValorDisponivel()).isEqualByComparingTo("3500.50");
        assertThat(response.getTiposAceitos()).containsExactlyInAnyOrder(TipoAtivo.ACAO, TipoAtivo.ETF);
        assertThat(response.getSetoresPreferidos()).hasSize(1);
        assertThat(response.getSetoresPreferidos().get(0).getPreferencia()).isEqualTo(PreferenciaSetor.EVITAR);
        assertThat(response.isPerfilPreenchido()).isTrue();
        assertThat(response.getResumoIA()).isEqualTo("Você busca preservar o capital no curto prazo, com perfil arrojado.");
    }

    @Test
    @DisplayName("obterPerfil - deve retornar campos nulos/vazios quando perfil ainda não foi preenchido")
    void obterPerfil_deveRetornarCamposNulosQuandoNaoPreenchido() {
        UUID usuarioId = UUID.randomUUID();
        PerfilInvestidor perfil = PerfilInvestidor.builder()
                .id(UUID.randomUUID())
                .perfilPreenchido(false)
                .build();

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(resumoPerfilService.gerarResumoIA(perfil)).thenReturn("Complete o quiz para receber sua análise personalizada.");

        PerfilResponseDTO response = perfilService.obterPerfil(usuarioId);

        assertThat(response.getPerfilRisco()).isNull();
        assertThat(response.getObjetivoFinanceiro()).isNull();
        assertThat(response.getHorizonteInvestimento()).isNull();
        assertThat(response.getTiposAceitos()).isEmpty();
        assertThat(response.getSetoresPreferidos()).isEmpty();
        assertThat(response.isPerfilPreenchido()).isFalse();
    }

    @Test
    @DisplayName("obterPerfil - deve lançar exceção quando perfil não encontrado")
    void obterPerfil_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perfilService.obterPerfil(usuarioId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Perfil do investidor não encontrado");
    }

    @Test
    @DisplayName("editarPerfil - deve atualizar todos os campos e marcar perfilPreenchido")
    void editarPerfil_deveAtualizarTodosOsCampos() {
        UUID usuarioId = UUID.randomUUID();
        PerfilInvestidor perfilExistente = PerfilInvestidor.builder()
                .id(UUID.randomUUID())
                .perfilPreenchido(false)
                .build();

        EditarPerfilRequestDTO dto = new EditarPerfilRequestDTO();
        dto.setPerfilRisco(PerfilRisco.MODERADO);
        dto.setHorizonteInvestimento(HorizonteInvestimento.LONGO_PRAZO);
        dto.setObjetivoFinanceiro(ObjetivoFinanceiro.RENDA_PASSIVA);
        dto.setValorDisponivel(new BigDecimal("10000.00"));
        dto.setTiposAceitos(List.of(TipoAtivo.FII));
        SetorPreferidoRequestDTO setor = new SetorPreferidoRequestDTO();
        setor.setSetor("Financeiro e Bancos");
        setor.setPreferencia(PreferenciaSetor.PREFERIR);
        dto.setSetoresPreferidos(List.of(setor));

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilExistente));
        when(perfilInvestidorRepository.save(any(PerfilInvestidor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumoPerfilService.gerarResumoIA(any())).thenReturn("resumo qualquer");

        PerfilResponseDTO response = perfilService.editarPerfil(usuarioId, dto);

        ArgumentCaptor<PerfilInvestidor> captor = ArgumentCaptor.forClass(PerfilInvestidor.class);
        verify(perfilInvestidorRepository).save(captor.capture());
        PerfilInvestidor salvo = captor.getValue();

        assertThat(salvo.getPerfilRisco()).isEqualTo("MODERADO");
        assertThat(salvo.getHorizonte()).isEqualTo("LONGO_PRAZO");
        assertThat(salvo.getObjetivo()).isEqualTo("RENDA_PASSIVA");
        assertThat(salvo.getValorDisponivel()).isEqualByComparingTo("10000.00");
        assertThat(salvo.getTiposAceitos()).containsExactly("FII");
        assertThat(salvo.getSetoresPreferidos()).hasSize(1);
        assertThat(salvo.getSetoresPreferidos().get(0).getSetor()).isEqualTo("Financeiro e Bancos");
        assertThat(salvo.isPerfilPreenchido()).isTrue();

        assertThat(response.getValorDisponivel()).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("editarPerfil - deve substituir a lista de setores preferidos, não mesclar com a antiga")
    void editarPerfil_deveSubstituirSetoresPreferidos() {
        UUID usuarioId = UUID.randomUUID();
        PerfilInvestidor perfilExistente = perfilCompletoMock(UUID.randomUUID()); // já tem "Tecnologia" EVITAR

        EditarPerfilRequestDTO dto = new EditarPerfilRequestDTO();
        dto.setPerfilRisco(PerfilRisco.CONSERVADOR);
        dto.setHorizonteInvestimento(HorizonteInvestimento.CURTO_PRAZO);
        dto.setObjetivoFinanceiro(ObjetivoFinanceiro.PRESERVAR_CAPITAL);
        dto.setValorDisponivel(BigDecimal.ZERO);
        dto.setTiposAceitos(List.of(TipoAtivo.ACAO));
        dto.setSetoresPreferidos(List.of()); // limpa os setores

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfilExistente));
        when(perfilInvestidorRepository.save(any(PerfilInvestidor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumoPerfilService.gerarResumoIA(any())).thenReturn("resumo qualquer");

        perfilService.editarPerfil(usuarioId, dto);

        ArgumentCaptor<PerfilInvestidor> captor = ArgumentCaptor.forClass(PerfilInvestidor.class);
        verify(perfilInvestidorRepository).save(captor.capture());
        assertThat(captor.getValue().getSetoresPreferidos()).isEmpty();
    }

    @Test
    @DisplayName("editarPerfil - deve lançar exceção quando perfil não encontrado")
    void editarPerfil_deveLancarExcecaoQuandoNaoEncontrado() {
        UUID usuarioId = UUID.randomUUID();
        EditarPerfilRequestDTO dto = new EditarPerfilRequestDTO();
        dto.setPerfilRisco(PerfilRisco.MODERADO);
        dto.setHorizonteInvestimento(HorizonteInvestimento.LONGO_PRAZO);
        dto.setObjetivoFinanceiro(ObjetivoFinanceiro.RENDA_PASSIVA);
        dto.setValorDisponivel(BigDecimal.TEN);
        dto.setTiposAceitos(List.of(TipoAtivo.ACAO));
        dto.setSetoresPreferidos(List.of());

        when(perfilInvestidorRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perfilService.editarPerfil(usuarioId, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Perfil do investidor não encontrado");

        verify(perfilInvestidorRepository, never()).save(any());
    }
}