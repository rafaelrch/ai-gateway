package br.com.rafael.aigateway.core.chain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Quem "puxa" a corrente: percorre os handlers na ordem do @Order.
 *
 * Esta é a versão "lista ordenada" do Chain of Responsibility. Na versão
 * clássica, cada handler guarda uma referência para o próximo e chama
 * proximo.handle(...) no fim. Aqui, quem conhece a ordem é esta classe, e
 * cada handler só cuida da sua regra. Comparação completa em docs/PADROES.md.
 */
@Component
public class CorrenteDeValidacao {

    private static final Logger log = LoggerFactory.getLogger(CorrenteDeValidacao.class);

    // Todos os beans que implementam PromptHandler, já ordenados pelo @Order.
    private final List<PromptHandler> handlers;

    // O Spring acha todas as implementações, ordena e injeta a lista aqui.
    public CorrenteDeValidacao(List<PromptHandler> handlers) {
        this.handlers = handlers;
        // Mostra na subida a ordem real da corrente (útil para conferir o @Order).
        log.info("[chain] corrente montada: {}",
                handlers.stream().map(h -> h.getClass().getSimpleName()).toList());
    }

    // Passa o contexto por cada elo até o fim ou até alguém responder.
    public void executar(ContextoRequisicao ctx) {
        for (PromptHandler handler : handlers) {
            // Se o handler barrar, a exceção sobe daqui direto para o controller advice.
            handler.handle(ctx);
            // Se o handler respondeu (cache), não precisa rodar os próximos.
            if (ctx.foiRespondida()) {
                return;
            }
        }
    }
}
