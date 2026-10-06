package br.com.rafael.aigateway.core.chain;

import br.com.rafael.aigateway.core.PromptBloqueadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Elo 1 da corrente: barra prompt grande demais.
 *
 * Prompt grande custa caro (provedor cobra por token). Este handler estima
 * os tokens e barra antes de gastar dinheiro. É o primeiro da fila porque é
 * a checagem mais barata: só conta caracteres.
 */
@Component  // vira bean do Spring e entra automaticamente na List<PromptHandler>
@Order(1)   // posição na corrente: o Spring ordena a lista por este número
public class TamanhoHandler implements PromptHandler {

    // Logger da classe. Escreve no console com o nome da classe na frente.
    private static final Logger log = LoggerFactory.getLogger(TamanhoHandler.class);

    // Regra prática: em inglês, 1 token tem uns 4 caracteres. Para um mock basta.
    private static final int CARACTERES_POR_TOKEN = 4;

    // Limite de tokens, lido do application.properties (padrão 100 se faltar).
    private final int maxTokens;

    // O Spring chama este construtor e injeta o valor da propriedade.
    public TamanhoHandler(@Value("${gateway.chain.max-tokens:100}") int maxTokens) {
        this.maxTokens = maxTokens;
    }

    @Override
    public void handle(ContextoRequisicao ctx) {
        // Estima os tokens dividindo o tamanho do texto por 4 (divisão inteira).
        int tokens = ctx.requisicao().prompt().length() / CARACTERES_POR_TOKEN;
        // Deixa a estimativa no contexto para quem vier depois usar.
        ctx.registrarTokens(tokens);

        // Passou do limite: barra lançando a exceção. A corrente para aqui.
        if (tokens > maxTokens) {
            log.info("[chain] TamanhoHandler: {} tokens, barrado (limite {})", tokens, maxTokens);
            throw new PromptBloqueadoException(
                    "Prompt com " + tokens + " tokens. O limite é " + maxTokens + ".");
        }

        // Dentro do limite: só registra e retorna. A corrente segue.
        log.info("[chain] TamanhoHandler: {} tokens, aprovado", tokens);
    }
}
