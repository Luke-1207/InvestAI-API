package com.investai.api.module.dashboard.repository;

import com.investai.api.module.dashboard.entity.IndicadoresMercado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndicadoresMercadoRepository extends JpaRepository<IndicadoresMercado, UUID> {
    Optional<IndicadoresMercado> findTopByOrderByIdAsc();
}