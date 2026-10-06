package br.com.rafael.aigateway.core;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import br.com.rafael.aigateway.core.chain.ContextoRequisicao;
import br.com.rafael.aigateway.core.chain.CorrenteDeValidacao;
import br.com.rafael.aigateway.provider.AiProvider;
import br.com.rafael.aigateway.provider.ProviderRegistry;
import br.com.rafael.aigateway.provider.RespostaIa;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiGatewayService {

    private final CorrenteDeValidacao corrente;
    private final ProviderRegistry registry;
    private final CacheDeRespostas cache;

    public AiGatewayService(CorrenteDeValidacao corrente, ProviderRegistry registry, CacheDeRespostas cache) {
        this.corrente = corrente;
        this.registry = registry;
        this.cache = cache;
    }

    public CompletionResponse processar(CompletionRequest requisicao) {
        long inicio = System.currentTimeMillis();

        ContextoRequisicao ctx = new ContextoRequisicao(requisicao);
        corrente.executar(ctx);

        Optional<CompletionResponse> doCache = ctx.respostaPronta();
        if (doCache.isPresent()) {
            CompletionResponse original = doCache.get();
            return new CompletionResponse(
                    UUID.randomUUID().toString(),
                    original.resposta(),
                    original.provedorUsado(),
                    0,
                    BigDecimal.ZERO,
                    true,
                    System.currentTimeMillis() - inicio);
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
        return completionResponse;
    }
}
