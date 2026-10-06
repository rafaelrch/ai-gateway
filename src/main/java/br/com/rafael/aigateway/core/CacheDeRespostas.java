package br.com.rafael.aigateway.core;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache em memória das respostas já geradas.
 *
 * É lido pelo CacheHandler (na corrente) e escrito pelo AiGatewayService
 * (depois que o provedor responde). Por isso virou uma classe própria em vez
 * de ficar dentro de um dos dois.
 *
 * SINGLETON NA PRÁTICA: este bean existe uma vez só na aplicação inteira, e
 * várias requisições (threads) mexem no mesmo mapa ao mesmo tempo. Por isso o
 * mapa é um ConcurrentHashMap, que aguenta leitura e escrita simultâneas.
 * Um HashMap comum aqui poderia corromper os dados sob carga.
 *
 * Limitação conhecida: o mapa cresce sem limite e não expira. Para produção
 * seria Caffeine ou Redis com tempo de vida.
 */
@Component
public class CacheDeRespostas {

    // chave "PERFIL:prompt" -> resposta original gerada pelo provedor.
    private final Map<String, CompletionResponse> respostas = new ConcurrentHashMap<>();

    // Busca uma resposta guardada. Optional vazio = nunca vimos este pedido.
    public Optional<CompletionResponse> buscar(CompletionRequest requisicao) {
        return Optional.ofNullable(respostas.get(chave(requisicao)));
    }

    // Guarda a resposta para a próxima vez que o mesmo pedido chegar.
    public void guardar(CompletionRequest requisicao, CompletionResponse resposta) {
        respostas.put(chave(requisicao), resposta);
    }

    // Perfil entra na chave porque o mesmo prompt no PREMIUM e no RAPIDO são respostas diferentes.
    private String chave(CompletionRequest requisicao) {
        return requisicao.perfil() + ":" + requisicao.prompt().strip();
    }
}
