package com.investai.api.module.rendafixa.repository;

import com.investai.api.module.rendafixa.entity.TituloTesouro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TituloTesouroRepository extends JpaRepository<TituloTesouro, UUID> {
    boolean existsByCodigo(String codigo);

    Optional<TituloTesouro> findByCodigo(String codigo);

    List<TituloTesouro> findByDisponivelTrue();
}