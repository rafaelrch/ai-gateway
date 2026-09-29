package br.com.rafael.aigateway.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CompletionRequest(
        @NotBlank String prompt,
        String perfil) {
}
