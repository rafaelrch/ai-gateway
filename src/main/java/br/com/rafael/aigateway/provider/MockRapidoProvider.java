package br.com.rafael.aigateway.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static br.com.rafael.aigateway.provider.Perfil.RAPIDO;

@Component
public class MockRapidoProvider implements AiProvider{

    @Override
    public RespostaIa gerar(String prompt) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Provedor interrompido", e);
        }

        int tokens = prompt.length() / 4;
        BigDecimal custoEstimado = new BigDecimal("0.12");

        return new RespostaIa("Texto para RAPIDO 2", tokens, custoEstimado);
    }

    @Override
    public String nome() {
        return "MOCK_RAPIDO";
    }

    @Override
    public Perfil perfil(){
        return RAPIDO;
    }
}
