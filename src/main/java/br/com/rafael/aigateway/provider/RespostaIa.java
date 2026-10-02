package br.com.rafael.aigateway.provider;

import java.math.BigDecimal;

public record RespostaIa(String texto, int tokens, BigDecimal custo) {
}
