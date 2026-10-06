package br.com.rafael.aigateway.provider;

/**
 * Falha temporária do provedor (timeout, 503, rede caiu).
 *
 * "Temporária" é o ponto: vale a pena tentar de novo. O RetryProviderDecorator
 * só repete quando recebe ESTA exceção. Se todas as tentativas falharem, ela
 * chega ao GlobalExceptionHandler e vira 503 Service Unavailable.
 */
public class ProvedorIndisponivelException extends RuntimeException {

    public ProvedorIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
