package com.investai.api.module.relatorio.service;

import com.investai.api.module.ativo.dto.AcaoDetalheResponseDTO;
import com.investai.api.module.ativo.dto.PeriodoHistorico;
import com.investai.api.module.ativo.service.AcaoDetalheService;
import com.investai.api.module.ativo.service.AcaoSugestaoService;
import com.investai.api.module.ativo.service.ResumoAtivoService;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.relatorio.builder.RelatorioAtivoBuilder;
import com.investai.api.module.relatorio.builder.RelatorioListagemBuilder;
import com.investai.api.module.relatorio.builder.RelatorioPerfilBuilder;
import com.investai.api.module.relatorio.dto.DadosRelatorioAtivoFixo;
import com.investai.api.module.relatorio.dto.DadosRelatorioAtivoVariavel;
import com.investai.api.module.relatorio.dto.DadosRelatorioListagem;
import com.investai.api.module.relatorio.dto.DadosRelatorioPerfil;
import com.investai.api.module.relatorio.dto.ModuloRelatorio;
import com.investai.api.module.relatorio.dto.RelatorioGeradoDTO;
import com.investai.api.module.relatorio.dto.RelatorioListagemRequestDTO;
import com.investai.api.module.relatorio.dto.TituloRendaFixaRelatorio;
import com.investai.api.module.relatorio.entity.TipoRelatorio;
import com.investai.api.module.perfil.service.PerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    public static final String PREFIXO_REFERENCIA_LISTAGEM = "LISTAGEM_";
    public static final String REFERENCIA_PERFIL = "PERFIL";

    private static final String NOME_ARQUIVO_PERFIL = "perfil-investidor.pdf";

    private static final Map<ModuloRelatorio, String> NOMES_ARQUIVO_LISTAGEM = Map.of(
            ModuloRelatorio.VARIAVEL, "listagem-renda-variavel.pdf",
            ModuloRelatorio.FIXA, "listagem-renda-fixa.pdf",
            ModuloRelatorio.AMBOS, "listagem-completa.pdf");

    private final AcaoDetalheService acaoDetalheService;
    private final AcaoSugestaoService acaoSugestaoService;
    private final ResumoAtivoService resumoAtivoService;
    private final PerfilService perfilService;
    private final RelatorioAtivoBuilder relatorioAtivoBuilder;
    private final RelatorioRendaFixaService relatorioRendaFixaService;
    private final RelatorioListagemService relatorioListagemService;
    private final RelatorioListagemBuilder relatorioListagemBuilder;
    private final RelatorioPerfilBuilder relatorioPerfilBuilder;
    private final HistoricoRelatorioService historicoRelatorioService;

    public RelatorioGeradoDTO gerarRelatorioAtivoVariavel(String codigo, Usuario usuario) {
        AcaoDetalheResponseDTO ativo = acaoDetalheService.obterDetalhe(codigo, PeriodoHistorico.UM_ANO.getCodigo());

        DadosRelatorioAtivoVariavel dados = DadosRelatorioAtivoVariavel.builder()
                .nomeUsuario(usuario.getNome())
                .geradoEm(LocalDateTime.now())
                .perfil(perfilService.obterPerfil(usuario.getId()))
                .ativo(ativo)
                .compatibilidade(acaoSugestaoService.obterSugestao(ativo.getCodigo(), usuario.getId()))
                .resumoIa(resumoAtivoService.obterResumo(usuario.getId(), ativo).orElse(null))
                .build();

        byte[] pdf = relatorioAtivoBuilder.construir(dados);
        historicoRelatorioService.registrar(usuario.getId(), TipoRelatorio.ATIVO_INDIVIDUAL, ativo.getCodigo());

        return new RelatorioGeradoDTO("analise-" + ativo.getCodigo() + ".pdf", pdf);
    }

    public RelatorioGeradoDTO gerarRelatorioAtivoFixo(String identificador, Usuario usuario) {
        TituloRendaFixaRelatorio titulo = relatorioRendaFixaService.buscarTitulo(identificador);

        DadosRelatorioAtivoFixo dados = DadosRelatorioAtivoFixo.builder()
                .nomeUsuario(usuario.getNome())
                .geradoEm(LocalDateTime.now())
                .perfil(perfilService.obterPerfil(usuario.getId()))
                .titulo(titulo)
                .build();

        byte[] pdf = relatorioAtivoBuilder.construir(dados);
        historicoRelatorioService.registrar(usuario.getId(), TipoRelatorio.ATIVO_INDIVIDUAL, titulo.nome());

        return new RelatorioGeradoDTO("analise-" + paraNomeDeArquivo(titulo.identificador()) + ".pdf", pdf);
    }

    public RelatorioGeradoDTO gerarRelatorioListagem(RelatorioListagemRequestDTO request, Usuario usuario) {
        DadosRelatorioListagem dados = relatorioListagemService.montarDados(request, usuario);

        byte[] pdf = relatorioListagemBuilder.construir(dados);
        historicoRelatorioService.registrar(
                usuario.getId(), TipoRelatorio.LISTAGEM, PREFIXO_REFERENCIA_LISTAGEM + request.getModulo().name());

        return new RelatorioGeradoDTO(NOMES_ARQUIVO_LISTAGEM.get(request.getModulo()), pdf);
    }

    public RelatorioGeradoDTO gerarRelatorioPerfil(Usuario usuario) {
        DadosRelatorioPerfil dados = DadosRelatorioPerfil.builder()
                .nomeUsuario(usuario.getNome())
                .email(usuario.getEmail())
                .telefone(usuario.getTelefone())
                .cadastradoEm(usuario.getCriadoEm())
                .geradoEm(LocalDateTime.now())
                .perfil(perfilService.obterPerfil(usuario.getId()))
                .build();

        byte[] pdf = relatorioPerfilBuilder.construir(dados);
        historicoRelatorioService.registrar(usuario.getId(), TipoRelatorio.PERFIL, REFERENCIA_PERFIL);

        return new RelatorioGeradoDTO(NOME_ARQUIVO_PERFIL, pdf);
    }

    private String paraNomeDeArquivo(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }
}
