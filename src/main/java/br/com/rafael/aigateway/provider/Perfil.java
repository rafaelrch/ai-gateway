package br.com.rafael.aigateway.provider;

/**
 * Os perfis que o cliente pode pedir no JSON ("perfil": "RAPIDO").
 *
 * O cliente escolhe o perfil (o "o quê"), nunca o provedor (o "quem").
 * Quem traduz perfil em provedor é o ProviderRegistry. Assim dá para trocar
 * o provedor de um perfil sem o cliente perceber.
 *
 * O Jackson converte o texto do JSON neste enum. Texto que não existe aqui
 * (ex.: "XYZ") vira 400 antes de chegar no controller.
 */
public enum Perfil {
    RAPIDO,     // resposta curta e barata, latência baixa
    PREMIUM,    // resposta melhor, mais lenta e mais cara
    ECONOMICO   // o mais barato de todos, mas instável (falha às vezes)
}
