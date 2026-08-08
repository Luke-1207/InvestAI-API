package com.investai.api.module.ativo.repository;

import com.investai.api.module.ativo.entity.Acao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcaoRepository extends JpaRepository<Acao, UUID> {
    boolean existsByCodigo(String codigo);

    List<Acao> findByAtivoTrue();
}
