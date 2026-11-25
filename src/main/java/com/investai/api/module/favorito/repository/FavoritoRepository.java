package com.investai.api.module.favorito.repository;

import com.investai.api.module.favorito.entity.Favorito;
import com.investai.api.module.favorito.entity.TipoAtivoFavorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, UUID> {

    List<Favorito> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);

    Optional<Favorito> findByUsuarioIdAndAtivoIdAndTipoAtivo(UUID usuarioId, UUID ativoId, TipoAtivoFavorito tipoAtivo);

    boolean existsByUsuarioIdAndAtivoIdAndTipoAtivo(UUID usuarioId, UUID ativoId, TipoAtivoFavorito tipoAtivo);

    long countByUsuarioId(UUID usuarioId);

    List<Favorito> findByTipoAtivoAndAlertasAtivosTrue(TipoAtivoFavorito tipoAtivo);
}
