package br.com.rafael.aigateway.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * PADRÃO: Strategy (uma estratégia concreta) e a "dor" do Decorator.
 *
 * Provedor falso barato e instável: falha em 40% das chamadas (configurável).
 * É ele que justifica o RetryProviderDecorator.
 *
 * Também é a prova do princípio aberto/fechado do Strategy: este provedor e
 * o perfil ECONOMICO foram adicionados sem mudar uma linha do AiGatewayService.
 */
@Component
public class MockEconomicoProvider implements AiProvider {

    // Metade do preço do rápido.
    private static final BigDecimal PRECO_POR_MIL_TOKENS = new BigDecimal("0.01");

    // Chance de falhar em cada chamada, de 0.0 (nunca) a 1.0 (sempre).
    private final double taxaDeFalha;

    // Lida do application.properties. Nos testes dá para pôr 0 ou 1 e ter resultado previsível.
    public MockEconomicoProvider(@Value("${gateway.provider.economico.taxa-falha:0.4}") double taxaDeFalha) {
        this.taxaDeFalha = taxaDeFalha;
    }

    @Override
    public RespostaIa gerar(String prompt) {
        LatenciaSimulada.esperar(150);

        // Sorteia um número entre 0 e 1. Abaixo da taxa, simula queda do provedor.
        // ThreadLocalRandom: um gerador por thread, sem disputa entre requisições.
        if (ThreadLocalRandom.current().nextDouble() < taxaDeFalha) {
            throw new ProvedorIndisponivelException(nome() + " não respondeu (falha simulada)");
        }

        String texto = "[" + nome() + "] Resposta econômica para: " + prompt;
        int tokens = LatenciaSimulada.tokens(prompt) + LatenciaSimulada.tokens(texto);

        return new RespostaIa(texto, tokens, LatenciaSimulada.custo(tokens, PRECO_POR_MIL_TOKENS));
    }

    @Override
    public String nome() {
        return "MOCK_ECONOMICO";
    }

    @Override
    public Perfil perfil() {
        return Perfil.ECONOMICO;
    }
}
