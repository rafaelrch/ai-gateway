package br.com.rafael.aigateway.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PADRÃO: Strategy (uma estratégia concreta).
 *
 * Provedor falso que simula um modelo rápido e barato: 100 ms de latência.
 * @Component faz o Spring criar um bean dele na subida. Como implementa
 * AiProvider, ele entra sozinho na List<AiProvider> que o registry recebe.
 */
@Component
public class MockRapidoProvider implements AiProvider {

    // Preço por mil tokens, em reais. Criado a partir de String para ser exato.
    private static final BigDecimal PRECO_POR_MIL_TOKENS = new BigDecimal("0.02");

    @Override
    public RespostaIa gerar(String prompt) {
        // Simula a ida e volta na rede.
        LatenciaSimulada.esperar(100);

        // Monta um texto fixo que mostra quem respondeu e o que foi pedido.
        String texto = "[" + nome() + "] Resposta curta para: " + prompt;
        // Tokens cobrados = os do prompt mais os da resposta.
        int tokens = LatenciaSimulada.tokens(prompt) + LatenciaSimulada.tokens(texto);

        // Devolve só o que o provedor sabe: texto, tokens e custo.
        return new RespostaIa(texto, tokens, LatenciaSimulada.custo(tokens, PRECO_POR_MIL_TOKENS));
    }

    @Override
    public String nome() {
        return "MOCK_RAPIDO";
    }

    @Override
    public Perfil perfil() {
        return Perfil.RAPIDO;
    }
}
