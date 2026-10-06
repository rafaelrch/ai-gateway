package br.com.rafael.aigateway.usage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * O que o GET /usage devolve: números agregados do histórico.
 *
 * O cálculo mora aqui (no método de fábrica de()) e não no controller, para
 * o controller continuar magro: só recebe e devolve.
 */
public record ResumoUso(
        int totalRequisicoes,               // requisições respondidas (barradas não entram)
        long cacheHits,                     // quantas vieram do cache
        long totalTokens,                   // soma de tokens
        BigDecimal custoTotal,              // soma de custo em reais
        Map<String, Long> porProvedor,      // provedor -> número de requisições
        List<UsoRegistradoEvent> ultimas) { // as 10 mais recentes, da mais nova para a mais velha

    // Quantas requisições recentes aparecem no resumo.
    private static final int ULTIMAS = 10;

    // Método de fábrica estático: monta o resumo a partir da lista de registros.
    public static ResumoUso de(List<UsoRegistradoEvent> registros) {
        // Conta só os que vieram do cache.
        long hits = registros.stream().filter(UsoRegistradoEvent::cacheHit).count();

        // Soma os tokens de todos (mapToLong evita estourar int).
        long tokens = registros.stream().mapToLong(UsoRegistradoEvent::tokens).sum();

        // Soma os custos. reduce começa em ZERO e vai somando um a um.
        BigDecimal custo = registros.stream()
                .map(UsoRegistradoEvent::custo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Agrupa por nome do provedor e conta. TreeMap deixa em ordem alfabética.
        Map<String, Long> porProvedor = registros.stream()
                .collect(Collectors.groupingBy(UsoRegistradoEvent::provedor, TreeMap::new, Collectors.counting()));

        // Pega os últimos N e inverte, para o mais recente vir primeiro.
        List<UsoRegistradoEvent> ultimas = registros
                .subList(Math.max(0, registros.size() - ULTIMAS), registros.size())
                .reversed();

        return new ResumoUso(registros.size(), hits, tokens, custo, porProvedor, ultimas);
    }
}
