package br.com.rafael.aigateway.provider;

public interface AiProvider{

    RespostaIa gerar(String prompt);
    String nome();

}
