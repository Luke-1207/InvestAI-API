package com.investai.api.module.perfil.repository;

import com.investai.api.module.perfil.entity.PerfilInvestidor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PerfilInvestidorRepository extends JpaRepository<PerfilInvestidor, UUID> {
    Optional<PerfilInvestidor> findByUsuarioId(UUID usuarioId);
}
