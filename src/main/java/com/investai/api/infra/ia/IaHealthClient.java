package com.investai.api.infra.ia;

import com.investai.api.infra.ia.dto.IaHealthStatusDTO;

public interface IaHealthClient {
    IaHealthStatusDTO verificarStatus();
}