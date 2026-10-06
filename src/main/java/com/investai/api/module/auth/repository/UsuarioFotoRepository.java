package com.investai.api.module.auth.repository;

import com.investai.api.module.auth.entity.UsuarioFoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UsuarioFotoRepository extends JpaRepository<UsuarioFoto, UUID> {
}