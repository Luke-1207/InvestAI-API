package com.investai.api.module.auth.repository;

import com.investai.api.module.auth.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query(value = """
    SELECT * FROM usuarios u
    WHERE u.deletado_em IS NULL
    AND (
        CAST(:busca AS TEXT) IS NULL OR
        u.nome ILIKE CONCAT('%', CAST(:busca AS TEXT), '%') OR
        u.email ILIKE CONCAT('%', CAST(:busca AS TEXT), '%')
    )
    AND (CAST(:role AS TEXT) IS NULL OR u.role = CAST(:role AS TEXT))
    AND (CAST(:ativo AS TEXT) IS NULL OR u.ativo = CAST(:ativo AS BOOLEAN))
    ORDER BY u.nome ASC
    """,
            countQuery = """
    SELECT COUNT(*) FROM usuarios u
    WHERE u.deletado_em IS NULL
    AND (
        CAST(:busca AS TEXT) IS NULL OR
        u.nome ILIKE CONCAT('%', CAST(:busca AS TEXT), '%') OR
        u.email ILIKE CONCAT('%', CAST(:busca AS TEXT), '%')
    )
    AND (CAST(:role AS TEXT) IS NULL OR u.role = CAST(:role AS TEXT))
    AND (CAST(:ativo AS TEXT) IS NULL OR u.ativo = CAST(:ativo AS BOOLEAN))
    """,
            nativeQuery = true)
    Page<Usuario> buscarComFiltros(
            @Param("busca") String busca,
            @Param("role") String role,
            @Param("ativo") Boolean ativo,
            Pageable pageable
    );
}
