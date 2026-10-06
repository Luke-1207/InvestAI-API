package com.investai.api.module.relatorio.repository;

import com.investai.api.module.relatorio.entity.HistoricoRelatorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HistoricoRelatorioRepository extends JpaRepository<HistoricoRelatorio, UUID> {
    Page<HistoricoRelatorio> findByUsuarioIdOrderByGeradoEmDesc(UUID usuarioId, Pageable pageable);
}
