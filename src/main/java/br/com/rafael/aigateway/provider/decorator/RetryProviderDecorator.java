package br.com.rafael.aigateway.provider.decorator;

import br.com.rafael.aigateway.provider.AiProvider;
import br.com.rafael.aigateway.provider.ProvedorIndisponivelException;
import br.com.rafael.aigateway.provider.RespostaIa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PADRÃO: Decorator (camada de nova tentativa).
 *
 * Se o provedor de dentro falhar com ProvedorIndisponivelException, tenta de
 * novo, até o máximo configurado. Nenhum provedor precisa saber que existe
 * retry, e nenhum provedor tem código de retry copiado.
 *
 * Não é @Component de propósito: se fosse, o Spring o colocaria na
 * List<AiProvider> como se fosse mais um provedor. Quem cria as camadas é o
 * ProviderRegistry, na subida.
 */
public class RetryProviderDecorator extends ProviderDecorator {

    private static final Logger log = LoggerFactory.getLogger(RetryProviderDecorator.class);

    // Quantas vezes, no total, o provedor pode ser chamado (3 = 1 normal + 2 repetições).
    private final int maxTentativas;

    public RetryProviderDecorator(AiProvider envolvido, int maxTentativas) {
        // Passa o envolvido para a classe base guardar.
        super(envolvido);
        // Pelo menos 1 tentativa, senão o provedor nunca seria chamado.
        if (maxTentativas < 1) {
            throw new IllegalArgumentException("maxTentativas precisa ser pelo menos 1");
        }
        this.maxTentativas = maxTentativas;
    }

    @Override
    public RespostaIa gerar(String prompt) {
        // Guarda a última falha para relançar se todas as tentativas falharem.
        ProvedorIndisponivelException ultimaFalha = null;

        // Tenta de 1 até maxTentativas.
        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            log.info("[decorator] {} tentativa {} de {}", nome(), tentativa, maxTentativas);
            try {
                // Repassa a chamada para a camada de dentro. Deu certo: devolve na hora.
                return envolvido.gerar(prompt);
            } catch (ProvedorIndisponivelException e) {
                // Falha temporária: anota e deixa o for tentar de novo.
                log.warn("[decorator] {} falhou: {}", nome(), e.getMessage());
                ultimaFalha = e;
            }
            // Qualquer outra exceção (ex.: bug) não é pega aqui e sobe direto, sem retry.
        }

        // Esgotou as tentativas: desiste e relança a última falha (vira 503).
        throw ultimaFalha;
    }
}
