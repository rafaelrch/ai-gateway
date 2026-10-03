package br.com.rafael.aigateway.api.dto;

import br.com.rafael.aigateway.provider.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompletionRequest(
        @NotBlank String prompt,
        @NotNull Perfil perfil) {
}
