package br.com.rafael.aigateway.core;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import br.com.rafael.aigateway.provider.AiProvider;
import br.com.rafael.aigateway.provider.ProviderRegistry;
import br.com.rafael.aigateway.provider.RespostaIa;
import org.springframework.stereotype.Service;

@Service
public class AiGatewayService {

    private final ProviderRegistry registry;

    public AiGatewayService(ProviderRegistry provider) {
        this.registry = provider;
    }

    public CompletionResponse processar(CompletionRequest completionRequest){

        long horaInicio = System.currentTimeMillis();
        long horaFim;

        AiProvider providerEscolhido = registry.obter(completionRequest.perfil());

        RespostaIa resposta = providerEscolhido.gerar(completionRequest.prompt());
        horaFim = System.currentTimeMillis();
        return new CompletionResponse(
                "teste2",
                resposta.texto(),
                providerEscolhido.nome(),
                resposta.tokens(),
                resposta.custo(),
                false,
                horaFim - horaInicio);
    }
}
