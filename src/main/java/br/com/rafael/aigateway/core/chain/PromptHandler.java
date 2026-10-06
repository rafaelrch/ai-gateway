package br.com.rafael.aigateway.core.chain;

/**
 * PADRÃO: Chain of Responsibility (o "elo" da corrente).
 *
 * Cada regra que o prompt precisa passar antes de chegar na IA implementa
 * esta interface. O gateway não conhece as regras uma por uma: ele recebe
 * todas numa lista ordenada e chama {@link #handle} em cada uma.
 *
 * Um handler tem três saídas possíveis:
 * 1. Aprovar: não faz nada e retorna. A corrente segue para o próximo.
 * 2. Barrar: lança {@link br.com.rafael.aigateway.core.PromptBloqueadoException}.
 *    A corrente para e a requisição volta 400 sem tocar no provedor.
 * 3. Responder: grava uma resposta pronta no contexto
 *    ({@link ContextoRequisicao#responderComCache}). A corrente para e o
 *    gateway devolve essa resposta sem chamar o provedor (é o caso do cache).
 *
 * Para criar uma regra nova basta escrever uma classe que implementa esta
 * interface e anotar com @Component e @Order. Nenhuma outra classe muda
 * (princípio aberto/fechado).
 */
public interface PromptHandler {

    // Recebe o contexto da requisição atual e decide: aprova, barra ou responde.
    void handle(ContextoRequisicao ctx);
}
