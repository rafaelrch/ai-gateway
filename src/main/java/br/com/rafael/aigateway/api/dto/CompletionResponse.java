package br.com.rafael.aigateway.api.dto;

import java.math.BigDecimal;

/**
 * O JSON que o gateway devolve.
 *
 * Mistura o que o provedor sabe (resposta, tokens, custo) com o que só o
 * gateway sabe (id, provedor escolhido, se veio do cache, duração total).
 */
public record CompletionResponse(
        String id,                  // UUID da requisição, o mesmo que aparece no GET /usage
        String resposta,            // texto gerado
        String provedorUsado,       // ex.: MOCK_RAPIDO. O cliente pediu um perfil, aqui vê quem atendeu
        int tokensGastos,           // tokens cobrados (0 se veio do cache)
        BigDecimal custoEstimado,   // custo em reais (0 se veio do cache). BigDecimal: dinheiro exato
        boolean cacheHit,           // true = respondido do cache, sem chamar provedor
        long duracaoMs) {           // tempo total dentro do gateway, em milissegundos
}
