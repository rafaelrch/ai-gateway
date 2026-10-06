package br.com.rafael.aigateway.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PADRÃO: Strategy (uma estratégia concreta).
 *
 * Provedor falso que simula um modelo grande: 800 ms e preço 10 vezes maior
 * que o rápido. Mesma interface, comportamento diferente: é isso que o
 * Strategy permite trocar sem o gateway saber.
 */
@Component
public class MockPremiumProvider implements AiProvider {

    // Dez vezes o preço do rápido.
    private static final BigDecimal PRECO_POR_MIL_TOKENS = new BigDecimal("0.20");

    @Override
    public RespostaIa gerar(String prompt) {
        // Modelo grande demora mais.
        LatenciaSimulada.esperar(800);

        // Texto um pouco mais elaborado, para diferenciar do rápido.
        String texto = "[" + nome() + "] Resposta detalhada, com contexto e exemplos, para: " + prompt;
        int tokens = LatenciaSimulada.tokens(prompt) + LatenciaSimulada.tokens(texto);

        return new RespostaIa(texto, tokens, LatenciaSimulada.custo(tokens, PRECO_POR_MIL_TOKENS));
    }

    @Override
    public String nome() {
        return "MOCK_PREMIUM";
    }

    @Override
    public Perfil perfil() {
        return Perfil.PREMIUM;
    }
}
