package br.com.rafael.aigateway.provider;

import java.math.BigDecimal;

/**
 * O que um provedor devolve: só o que ele sabe.
 *
 * O provedor sabe o texto, quantos tokens gastou e quanto custou. Ele NÃO
 * sabe id da requisição, se veio do cache nem a duração total: isso é
 * assunto do gateway, que monta o CompletionResponse.
 *
 * record: classe imutável com construtor, getters (texto(), tokens(),
 * custo()), equals, hashCode e toString gerados pelo compilador.
 */
public record RespostaIa(
        String texto,        // texto gerado pela "IA"
        int tokens,          // tokens consumidos (prompt + resposta)
        BigDecimal custo) {  // custo em reais. BigDecimal porque dinheiro não pode ter erro de arredondamento
}
