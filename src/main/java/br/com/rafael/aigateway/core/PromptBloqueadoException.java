package br.com.rafael.aigateway.core;

/**
 * Lançada por um elo da corrente quando o prompt não pode seguir para a IA.
 *
 * É unchecked (estende RuntimeException) para atravessar a corrente e o
 * service sem obrigar ninguém a declarar throws. Quem trata é o
 * GlobalExceptionHandler, que transforma em resposta 400.
 */
public class PromptBloqueadoException extends RuntimeException {

    // motivo: texto legível que vai parar no campo "detail" da resposta de erro.
    public PromptBloqueadoException(String motivo) {
        super(motivo);
    }
}
