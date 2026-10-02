package br.com.rafael.aigateway.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MockPremiumProvider implements AiProvider{

    @Override
    public RespostaIa gerar(String prompt) {
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Provedor interrompido", e);
        }

        int tokens = prompt.length() / 4;
        BigDecimal custoEstimado = new BigDecimal("2.12");

        return new RespostaIa("Texto para PREMIUM 2", tokens, custoEstimado);
    }

    @Override
    public String nome() {
        return "MOCK_PREMIUM";
    }

}
