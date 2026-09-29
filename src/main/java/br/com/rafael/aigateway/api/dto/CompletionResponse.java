package br.com.rafael.aigateway.api.dto;

import java.math.BigDecimal;

public record CompletionResponse(
        String id,
        String resposta,
        String provedorUsado,
        int tokensGastos,
        BigDecimal custoEstimado,
        boolean cacheHit,
        long duracaoMs) {

}
