package br.com.rafael.aigateway.provider.decorator;

import br.com.rafael.aigateway.provider.AiProvider;
import br.com.rafael.aigateway.provider.RespostaIa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PADRÃO: Decorator (camada de log e medição de tempo).
 *
 * Mede quanto tempo a camada de dentro levou e registra sucesso ou falha.
 * Fica POR FORA do retry: assim o tempo medido inclui todas as tentativas,
 * que é o tempo real que o cliente esperou.
 *
 * Empilhamento montado pelo ProviderRegistry:
 *   LoggingProviderDecorator( RetryProviderDecorator( MockXProvider ) )
 */
public class LoggingProviderDecorator extends ProviderDecorator {

    private static final Logger log = LoggerFactory.getLogger(LoggingProviderDecorator.class);

    public LoggingProviderDecorator(AiProvider envolvido) {
        super(envolvido);
    }

    @Override
    public RespostaIa gerar(String prompt) {
        // Marca o início. nanoTime é o relógio certo para medir intervalo.
        long inicio = System.nanoTime();
        try {
            // Chama a camada de dentro (o retry, que chama o provedor).
            RespostaIa resposta = envolvido.gerar(prompt);
            // Sucesso: loga o tempo e os tokens.
            log.info("[decorator] {} concluído em {}ms, {} tokens",
                    nome(), decorrido(inicio), resposta.tokens());
            return resposta;
        } catch (RuntimeException e) {
            // Falha final: loga e relança. O log não "engole" o erro.
            log.error("[decorator] {} falhou após {}ms: {}", nome(), decorrido(inicio), e.getMessage());
            throw e;
        }
    }

    // Converte nanossegundos decorridos em milissegundos.
    private long decorrido(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000;
    }
}
