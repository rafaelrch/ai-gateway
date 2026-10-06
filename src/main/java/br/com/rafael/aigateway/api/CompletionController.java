package br.com.rafael.aigateway.api;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import br.com.rafael.aigateway.core.AiGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /completions: a porta de entrada do gateway.
 *
 * Controller magro de propósito: recebe o JSON, valida o formato e entrega
 * para a fachada. Nenhuma regra de negócio aqui. Se amanhã o gateway for
 * chamado por fila em vez de HTTP, só esta classe muda.
 *
 * @RestController = @Controller + @ResponseBody: o retorno dos métodos vira
 * JSON (pelo Jackson) em vez de nome de página.
 */
@RestController
public class CompletionController {

    // A fachada. É o único objeto do core que o controller conhece.
    private final AiGatewayService service;

    // Injeção por construtor: o Spring entrega o bean único do service.
    public CompletionController(AiGatewayService service) {
        this.service = service;
    }

    // @PostMapping: liga este método ao POST /completions.
    // @RequestBody: o Jackson converte o JSON do corpo em CompletionRequest.
    // @Valid: roda as anotações do record (@NotBlank, @NotNull). Se falhar, 400
    // sem nem entrar no método.
    @Operation(summary = "Gera uma resposta", description = "Passa pela corrente de validação, "
            + "escolhe o provedor pelo perfil e devolve a resposta com custo, tokens e se veio do cache.")
    @PostMapping("/completions")
    public CompletionResponse gerar(@RequestBody @Valid CompletionRequest requisicao) {
        return service.processar(requisicao);
    }
}
