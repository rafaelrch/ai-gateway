package br.com.rafael.aigateway.api;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class CompletionController {

    @PostMapping("/completions")
    public CompletionResponse gerar(@RequestBody @Valid CompletionRequest cr){
        CompletionResponse completionResponse = new CompletionResponse("c4f1a2b8", "Teste 1", "MOCK_RAPIDO", 2346, new BigDecimal("0.12"), false, 600);
        return completionResponse;
    }
}