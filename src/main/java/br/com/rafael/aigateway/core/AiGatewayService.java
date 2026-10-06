package br.com.rafael.aigateway.core;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import br.com.rafael.aigateway.core.chain.ContextoRequisicao;
import br.com.rafael.aigateway.core.chain.CorrenteDeValidacao;
import br.com.rafael.aigateway.provider.AiProvider;
import br.com.rafael.aigateway.provider.ProviderRegistry;
import br.com.rafael.aigateway.provider.RespostaIa;
import br.com.rafael.aigateway.usage.UsoRegistradoEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiGatewayService {

    private final CorrenteDeValidacao corrente;
    private final ProviderRegistry registry;
    private final CacheDeRespostas cache;
    private final ApplicationEventPublisher eventos;

    public AiGatewayService(CorrenteDeValidacao corrente, ProviderRegistry registry,
                            CacheDeRespostas cache, ApplicationEventPublisher eventos) {
        this.corrente = corrente;
        this.registry = registry;
        this.cache = cache;
        this.eventos = eventos;
    }

    public CompletionResponse processar(CompletionRequest requisicao) {
        long inicio = System.currentTimeMillis();

        ContextoRequisicao ctx = new ContextoRequisicao(requisicao);
        corrente.executar(ctx);

        Optional<CompletionResponse> doCache = ctx.respostaPronta();
        if (doCache.isPresent()) {
            CompletionResponse original = doCache.get();
            CompletionResponse respostaCache = new CompletionResponse(
                    UUID.randomUUID().toString(),
                    original.resposta(),
                    original.provedorUsado(),
                    0,
                    BigDecimal.ZERO,
                    true,
                    System.currentTimeMillis() - inicio);
            publicar(requisicao, respostaCache);
            return respostaCache;
        }

        AiProvider provedor = registry.obter(requisicao.perfil());
        RespostaIa resposta = provedor.gerar(requisicao.prompt());

        CompletionResponse completionResponse = new CompletionResponse(
                UUID.randomUUID().toString(),
                resposta.texto(),
                provedor.nome(),
                resposta.tokens(),
                resposta.custo(),
                false,
                System.currentTimeMillis() - inicio);
        cache.guardar(requisicao, completionResponse);
        publicar(requisicao, completionResponse);
        return completionResponse;
    }

    private void publicar(CompletionRequest requisicao, CompletionResponse resposta) {
        eventos.publishEvent(new UsoRegistradoEvent(
                resposta.id(), requisicao.perfil(), resposta.provedorUsado(), resposta.tokensGastos(),
                resposta.custoEstimado(), resposta.cacheHit(), resposta.duracaoMs(), Instant.now()));
    }
}
