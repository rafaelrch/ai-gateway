package br.com.rafael.aigateway.api.dto;

import br.com.rafael.aigateway.provider.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * O JSON que o cliente manda no POST /completions.
 *
 * Exemplo: { "prompt": "Explique injeção de dependência", "perfil": "RAPIDO" }
 *
 * record: imutável, e o Jackson sabe criar record pelo construtor.
 * DTO (Data Transfer Object): só carrega dados entre o cliente e a API.
 */
public record CompletionRequest(
        // @NotBlank: não pode ser null, vazio ("") nem só espaços ("   ").
        @NotBlank(message = "o prompt é obrigatório") String prompt,
        // @NotNull: o perfil precisa vir. Texto fora do enum o Jackson já recusa.
        @NotNull(message = "o perfil é obrigatório (RAPIDO, PREMIUM ou ECONOMICO)") Perfil perfil) {
}
