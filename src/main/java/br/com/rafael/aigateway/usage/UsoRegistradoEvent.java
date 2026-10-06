package br.com.rafael.aigateway.usage;

import br.com.rafael.aigateway.provider.Perfil;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * PADRÃO: Observer (o evento, a "notícia" que é publicada).
 *
 * Analogia: o sino da cozinha do restaurante. Quando o prato sai, o
 * cozinheiro toca o sino e volta a cozinhar. Ele não sabe quem ouviu: o
 * garçom leva o prato, o caixa anota, o gerente confere. Cada um reage do
 * seu jeito, e dá para contratar mais gente que ouve o sino sem mudar o
 * cozinheiro.
 *
 * Aqui o cozinheiro é o AiGatewayService, o sino é este evento, e quem ouve
 * são as classes com @EventListener (UsageListener, AlertaDeCustoListener).
 *
 * record: evento é um fato que já aconteceu, então não deve mudar depois.
 */
public record UsoRegistradoEvent(
        String requisicaoId,   // o mesmo id devolvido ao cliente
        Perfil perfil,         // perfil pedido
        String provedor,       // provedor que respondeu (ou que respondeu da 1ª vez, se cache)
        int tokens,            // tokens gastos (0 no cache hit)
        BigDecimal custo,      // custo em reais (0 no cache hit)
        boolean cacheHit,      // true se respondeu do cache
        long duracaoMs,        // tempo total da requisição
        Instant momento) {     // quando aconteceu (UTC)
}
