package com.investai.api.module.integracao.repository;

import com.investai.api.module.integracao.entity.Integracao;
import com.investai.api.module.integracao.entity.StatusIntegracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusIntegracaoRepository extends JpaRepository<StatusIntegracao, Integracao> {
}
