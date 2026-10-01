package br.com.rafael.aigateway.api.core;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AiGatewayService {
    public CompletionResponse processar(CompletionRequest completionRequest) throws InterruptedException {
        long horaInicio = System.currentTimeMillis();
        long horaFim;

        String id;
        String resposta;
        String provedorUsado;
        int tokensGastos;
        BigDecimal custoEstimado;
        boolean cacheHit;
        long duracaoMs;

        String prompt = completionRequest.prompt();

        if(completionRequest.perfil().equals("RAPIDO")){
            Thread.sleep(100);
            resposta = "teste para RAPIDO";
            provedorUsado = "MOCK_RAPIDO";
            custoEstimado = new BigDecimal("0.12");
            horaFim = System.currentTimeMillis();
        } else if (completionRequest.perfil().equals("PREMIUM")){
            Thread.sleep(800);
            resposta = "teste para PREMIUM";
            provedorUsado = "MOCK_PREMIUM";
            custoEstimado = new BigDecimal("2.12");
            horaFim = System.currentTimeMillis();
        }else{
            throw new IllegalArgumentException("Perfil Desconhecido");
        }

        tokensGastos = prompt.length() / 4;
        duracaoMs = horaFim - horaInicio;
        return new CompletionResponse("dasda2", resposta, provedorUsado,tokensGastos,custoEstimado, false, duracaoMs);
    }
}
