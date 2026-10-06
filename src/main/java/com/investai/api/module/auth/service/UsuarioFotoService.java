package com.investai.api.module.auth.service;

import com.investai.api.infra.exception.BusinessException;
import com.investai.api.infra.exception.ResourceNotFoundException;
import com.investai.api.module.auth.entity.Usuario;
import com.investai.api.module.auth.entity.UsuarioFoto;
import com.investai.api.module.auth.repository.UsuarioFotoRepository;
import com.investai.api.shared.security.UsuarioAutenticadoHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UsuarioFotoService {

    static final long TAMANHO_MAXIMO_BYTES = 2 * 1024 * 1024;

    private static final Map<String, byte[]> ASSINATURAS_POR_TIPO = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    private final UsuarioFotoRepository usuarioFotoRepository;
    private final UsuarioAutenticadoHelper usuarioAutenticadoHelper;

    @Transactional
    public void salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Nenhum arquivo enviado");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new BusinessException("A foto deve ter no máximo 2 MB");
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || !ASSINATURAS_POR_TIPO.containsKey(contentType)) {
            throw new BusinessException("Formato não suportado. Use JPG, PNG ou WEBP");
        }

        byte[] conteudo = lerBytes(arquivo);

        if (!assinaturaConfere(contentType, conteudo)) {
            throw new BusinessException("O conteúdo do arquivo não corresponde a uma imagem válida");
        }

        Usuario usuario = usuarioAutenticadoHelper.getUsuarioLogado();
        UsuarioFoto foto = usuarioFotoRepository.findById(usuario.getId())
                .orElseGet(() -> UsuarioFoto.builder().usuarioId(usuario.getId()).build());

        foto.setConteudo(conteudo);
        foto.setContentType(contentType);
        foto.setAtualizadoEm(LocalDateTime.now());
        usuarioFotoRepository.save(foto);
    }

    @Transactional(readOnly = true)
    public UsuarioFoto obter() {
        Usuario usuario = usuarioAutenticadoHelper.getUsuarioLogado();
        return usuarioFotoRepository.findById(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não possui foto de perfil"));
    }

    @Transactional
    public void remover() {
        Usuario usuario = usuarioAutenticadoHelper.getUsuarioLogado();
        if (usuarioFotoRepository.existsById(usuario.getId())) {
            usuarioFotoRepository.deleteById(usuario.getId());
        }
    }

    private byte[] lerBytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Não foi possível ler o arquivo enviado");
        }
    }

    private boolean assinaturaConfere(String contentType, byte[] conteudo) {
        byte[] assinatura = ASSINATURAS_POR_TIPO.get(contentType);
        if (conteudo.length < assinatura.length) return false;
        if (!Arrays.equals(Arrays.copyOf(conteudo, assinatura.length), assinatura)) return false;

        if ("image/webp".equals(contentType)) {
            return conteudo.length >= 12
                    && conteudo[8] == 'W' && conteudo[9] == 'E' && conteudo[10] == 'B' && conteudo[11] == 'P';
        }
        return true;
    }
}