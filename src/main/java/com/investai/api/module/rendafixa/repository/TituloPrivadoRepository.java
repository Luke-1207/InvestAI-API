package com.investai.api.module.rendafixa.repository;

import com.investai.api.module.rendafixa.entity.TituloPrivado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TituloPrivadoRepository extends JpaRepository<TituloPrivado, UUID> {
    List<TituloPrivado> findByAtivoTrue();
}