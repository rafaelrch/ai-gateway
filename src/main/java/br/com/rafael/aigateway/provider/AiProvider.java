package br.com.rafael.aigateway.provider;

/**
 * PADRÃO: Strategy (a interface da estratégia).
 *
 * Todo provedor de IA "encaixa" aqui, como toda broca encaixa no mesmo
 * mandril da furadeira. O gateway programa contra esta interface e nunca
 * contra MockRapidoProvider ou MockPremiumProvider diretamente. Por isso dá
 * para criar um provedor novo sem mexer no gateway.
 *
 * Também é a interface que os decorators implementam (ver pacote decorator):
 * um decorator "parece" um provedor, mas por dentro chama outro provedor.
 */
public interface AiProvider {

    // Gera a resposta para o prompt. É aqui que cada provedor faz o seu jeito.
    RespostaIa gerar(String prompt);

    // Nome técnico do provedor, devolvido ao cliente em "provedorUsado".
    String nome();

    // Qual perfil este provedor atende. O registry usa isso para montar o mapa.
    Perfil perfil();
}
