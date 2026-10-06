package br.com.rafael.aigateway.provider;

import java.math.BigDecimal;

/**
 * Provedor de mentira só para testes: responde na hora (sem sleep) e pode ser
 * configurado para falhar as N primeiras chamadas. Conta quantas vezes foi
 * chamado, para o teste conferir.
 */
public class ProvedorFalso implements AiProvider {

    private final String nome;
    private final Perfil perfil;
    // Quantas das primeiras chamadas devem falhar com ProvedorIndisponivelException.
    private final int falhasAntesDeResponder;
    // Contador de chamadas a gerar().
    private int chamadas;

    public ProvedorFalso(String nome, Perfil perfil, int falhasAntesDeResponder) {
        this.nome = nome;
        this.perfil = perfil;
        this.falhasAntesDeResponder = falhasAntesDeResponder;
    }

    // Atalho para um provedor que nunca falha.
    public ProvedorFalso(String nome, Perfil perfil) {
        this(nome, perfil, 0);
    }

    @Override
    public RespostaIa gerar(String prompt) {
        chamadas++;
        if (chamadas <= falhasAntesDeResponder) {
            throw new ProvedorIndisponivelException(nome + " falhou na chamada " + chamadas);
        }
        return new RespostaIa(nome + " respondeu", 10, new BigDecimal("0.001"));
    }

    @Override
    public String nome() {
        return nome;
    }

    @Override
    public Perfil perfil() {
        return perfil;
    }

    public int chamadas() {
        return chamadas;
    }
}
