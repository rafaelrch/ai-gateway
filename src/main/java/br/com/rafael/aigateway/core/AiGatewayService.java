package br.com.rafael.aigateway.core;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import br.com.rafael.aigateway.provider.AiProvider;
import br.com.rafael.aigateway.provider.ProviderRegistry;
import br.com.rafael.aigateway.provider.RespostaIa;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiGatewayService {

    private final ProviderRegistry registry;
    private final Map<String, CompletionResponse> cache = new ConcurrentHashMap<>();

    public AiGatewayService(ProviderRegistry registry) {
        this.registry = registry;
    }

    public CompletionResponse processar(CompletionRequest completionRequest){

        List<String> termosProibidos =new ArrayList<>();
        termosProibidos.add("senha");
        termosProibidos.add("cpf");
        termosProibidos.add("cartao de credito");

        long horaInicio = System.currentTimeMillis();
        long horaFim;

        int tokensGastos = completionRequest.prompt().length() / 4;
        if (tokensGastos > 100) {
            throw new PromptBloqueadoException("Prompt com " + tokensGastos + " tokens, o limite e 100");
        }

        String promptMinusculo = completionRequest.prompt().toLowerCase();
        for (String termo : termosProibidos) {
            if (promptMinusculo.contains(termo)) {
                throw new PromptBloqueadoException("Prompt contem termo bloqueado: " + termo);
            }
        }

        String chave = completionRequest.perfil() + ":" + completionRequest.prompt();
        CompletionResponse emCache = cache.get(chave);
        if (emCache != null) {
            horaFim = System.currentTimeMillis();
            return new CompletionResponse(
                    emCache.id(),
                    emCache.resposta(),
                    emCache.provedorUsado(),
                    0,
                    BigDecimal.ZERO,
                    true,
                    horaFim - horaInicio);
        }

        AiProvider providerEscolhido = registry.obter(completionRequest.perfil());

        RespostaIa resposta = providerEscolhido.gerar(completionRequest.prompt());
        horaFim = System.currentTimeMillis();

        CompletionResponse completionResponse = new CompletionResponse(
                "teste2",
                resposta.texto(),
                providerEscolhido.nome(),
                resposta.tokens(),
                resposta.custo(),
                false,
                horaFim - horaInicio);
        cache.put(chave, completionResponse);
        return completionResponse;
    }
}
