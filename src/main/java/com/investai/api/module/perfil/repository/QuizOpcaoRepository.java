package com.investai.api.module.perfil.repository;

import com.investai.api.module.perfil.entity.QuizOpcao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuizOpcaoRepository extends JpaRepository<QuizOpcao, UUID> {
}