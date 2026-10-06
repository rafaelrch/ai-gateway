package br.com.rafael.aigateway.core;

public class PromptBloqueadoException extends RuntimeException {
    public PromptBloqueadoException(String motivo) {
        super(motivo);
    }
}
