package br.com.rafael.aigateway.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitário dos mocks: simula o tempo de resposta e calcula tokens e custo.
 *
 * Existe para os três mocks não repetirem o mesmo try/catch e a mesma conta.
 * final + construtor privado = ninguém instancia nem herda; só métodos static.
 */
final class LatenciaSimulada {

    // Construtor privado: classe utilitária não deve ser instanciada.
    private LatenciaSimulada() {
    }

    // Faz a thread "dormir" pelo tempo indicado, como se esperasse a rede.
    static void esperar(long milissegundos) {
        try {
            Thread.sleep(milissegundos);
        } catch (InterruptedException e) {
            // Alguém pediu para esta thread parar. Restaura a flag de interrupção
            // (o catch apaga a flag) para quem estiver acima saber disso.
            Thread.currentThread().interrupt();
            // Transforma em unchecked para não espalhar throws pelo código.
            throw new IllegalStateException("Provedor interrompido", e);
        }
    }

    // Estimativa simples: 1 token a cada 4 caracteres.
    static int tokens(String texto) {
        return texto.length() / 4;
    }

    // custo = tokens x preço por mil tokens / 1000, com 6 casas decimais.
    static BigDecimal custo(int tokens, BigDecimal precoPorMilTokens) {
        return precoPorMilTokens
                .multiply(BigDecimal.valueOf(tokens))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
    }
}
