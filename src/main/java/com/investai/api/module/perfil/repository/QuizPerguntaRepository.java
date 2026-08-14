package com.investai.api.module.perfil.repository;

import com.investai.api.module.perfil.entity.QuizPergunta;
import org.hibernate.validator.constraints.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizPerguntaRepository extends JpaRepository<QuizPergunta, UUID> {
    List<QuizPergunta> findAllByAtivaTrueOrderByOrdemAsc();
}