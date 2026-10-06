package br.com.rafael.aigateway.core.chain;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;

import java.util.Optional;

/**
 * O "envelope" que passa de mão em mão pela corrente.
 *
 * Por que não passar só o CompletionRequest? Porque os handlers precisam
 * deixar informação uns para os outros e para o gateway: quantos tokens o
 * prompt tem (calculado pelo TamanhoHandler) e, se for o caso, uma resposta
 * pronta (gravada pelo CacheHandler).
 *
 * Um objeto destes é criado por requisição e morre com ela. Por isso ele
 * pode ter estado mutável sem problema de concorrência: nenhuma outra
 * thread enxerga este objeto.
 */
public class ContextoRequisicao {

    // A requisição original que chegou no controller. final: nunca troca.
    private final CompletionRequest requisicao;

    // Tokens estimados do prompt. Começa em 0 e é preenchido pelo TamanhoHandler.
    private int tokensEstimados;

    // Resposta que algum handler deu no lugar do provedor. null = ninguém respondeu.
    private CompletionResponse respostaPronta;

    // Construtor: o contexto nasce sempre com a requisição dentro.
    public ContextoRequisicao(CompletionRequest requisicao) {
        this.requisicao = requisicao;
    }

    // Devolve a requisição original para os handlers lerem prompt e perfil.
    public CompletionRequest requisicao() {
        return requisicao;
    }

    // Lê os tokens estimados (0 se o TamanhoHandler ainda não rodou).
    public int tokensEstimados() {
        return tokensEstimados;
    }

    // Grava os tokens estimados. Chamado pelo TamanhoHandler.
    public void registrarTokens(int tokens) {
        this.tokensEstimados = tokens;
    }

    // Um handler respondeu no lugar do provedor. É o "pare a corrente" da regra 3.
    public void responderComCache(CompletionResponse resposta) {
        this.respostaPronta = resposta;
    }

    // true quando algum handler já respondeu. A corrente checa isso depois de cada elo.
    public boolean foiRespondida() {
        return respostaPronta != null;
    }

    // Devolve a resposta pronta embrulhada em Optional (vazio se ninguém respondeu).
    public Optional<CompletionResponse> respostaPronta() {
        return Optional.ofNullable(respostaPronta);
    }
}
