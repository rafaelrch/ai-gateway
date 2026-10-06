package br.com.rafael.aigateway.provider;

/**
 * O perfil existe no enum, mas nenhum provedor foi registrado para ele.
 *
 * Unchecked para não obrigar throws em quem chama o registry. O
 * GlobalExceptionHandler transforma em 400.
 */
public class PerfilNaoSuportadoException extends RuntimeException {

    // Monta a mensagem com o perfil pedido, para o cliente saber o que errou.
    public PerfilNaoSuportadoException(Perfil perfil) {
        super("Nenhum provedor para o perfil " + perfil);
    }
}
