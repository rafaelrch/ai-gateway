package br.com.rafael.aigateway.api;

import br.com.rafael.aigateway.api.core.AiGatewayService;
import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class CompletionController {

    AiGatewayService service;
    @PostMapping("/completions")
    public CompletionResponse gerar(@RequestBody @Valid CompletionRequest cr){
        return service.processar(cr);
    }
}