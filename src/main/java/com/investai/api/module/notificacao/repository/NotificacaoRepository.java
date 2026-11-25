package com.investai.api.module.notificacao.repository;

import com.investai.api.module.notificacao.entity.Notificacao;
import com.investai.api.module.notificacao.entity.TipoNotificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    Page<Notificacao> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId, Pageable pageable);

    Page<Notificacao> findByUsuarioIdAndLidaOrderByCriadoEmDesc(UUID usuarioId, boolean lida, Pageable pageable);

    long countByUsuarioIdAndLidaFalse(UUID usuarioId);

    Optional<Notificacao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    boolean existsByUsuarioIdAndTipoAndAtivoIdAndCriadoEmAfter(UUID usuarioId, TipoNotificacao tipo, UUID ativoId, LocalDateTime desde);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true, n.lidaEm = :agora WHERE n.usuarioId = :usuarioId AND n.lida = false")
    int marcarTodasComoLidas(UUID usuarioId, LocalDateTime agora);
}
