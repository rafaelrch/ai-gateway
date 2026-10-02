package br.com.rafael.aigateway.core;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import br.com.rafael.aigateway.provider.AiProvider;
import br.com.rafael.aigateway.provider.RespostaIa;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiGatewayService {

    private final List<AiProvider> provedores;

    public AiGatewayService(List<AiProvider> provedores) {
        this.provedores = provedores;
    }

    public CompletionResponse processar(CompletionRequest completionRequest){

        long horaInicio = System.currentTimeMillis();
        long horaFim;

        AiProvider providerEscolhido = null;
        for(AiProvider provedor : provedores){
            if(provedor.nome().equals("MOCK_" + completionRequest.perfil())){
                providerEscolhido = provedor;
                break;
            }
        }

        if (providerEscolhido == null){
            throw new IllegalArgumentException("Perfil desconhecido");
        }

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
