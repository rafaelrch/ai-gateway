package br.com.rafael.aigateway.core.chain;

import br.com.rafael.aigateway.api.dto.CompletionResponse;
import br.com.rafael.aigateway.core.CacheDeRespostas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Elo 3 da corrente: responde do cache quando o mesmo prompt já foi feito.
 *
 * É o exemplo de handler que não barra, mas encerra a corrente com uma
 * resposta. Fica por último de propósito: não faz sentido guardar ou
 * devolver do cache um prompt que as regras anteriores barrariam.
 */
@Component
@Order(3)   // último elo
public class CacheHandler implements PromptHandler {

    private static final Logger log = LoggerFactory.getLogger(CacheHandler.class);

    // O cache em si. Ele é compartilhado com o AiGatewayService, que grava nele.
    private final CacheDeRespostas cache;

    // Injeção por construtor: o Spring entrega o bean único de CacheDeRespostas.
    public CacheHandler(CacheDeRespostas cache) {
        this.cache = cache;
    }

    @Override
    public void handle(ContextoRequisicao ctx) {
        // Procura uma resposta anterior para o mesmo perfil + prompt.
        Optional<CompletionResponse> anterior = cache.buscar(ctx.requisicao());

        // Achou: grava no contexto. A corrente vê foiRespondida() == true e para.
        if (anterior.isPresent()) {
            log.info("[chain] CacheHandler: hit, respondendo sem chamar provedor");
            ctx.responderComCache(anterior.get());
            return;
        }

        // Não achou: segue para o provedor.
        log.info("[chain] CacheHandler: miss, seguindo");
    }
}
