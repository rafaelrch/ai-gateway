package br.com.rafael.aigateway.api;

import br.com.rafael.aigateway.usage.ResumoUso;
import br.com.rafael.aigateway.usage.UsageRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /usage: mostra quanto o gateway já gastou, com qual provedor e
 * quantas vezes o cache economizou uma chamada.
 *
 * Os dados chegam aqui pelo Observer: o service publica, o UsageListener
 * grava no UsageRepository, e este controller só lê.
 */
@RestController
public class UsageController {

    private final UsageRepository repository;

    public UsageController(UsageRepository repository) {
        this.repository = repository;
    }

    // Lê o histórico e devolve o resumo calculado. Nenhuma regra aqui.
    @Operation(summary = "Resumo de uso", description = "Total de requisições, cache hits, tokens, "
            + "custo e contagem por provedor, mais as 10 requisições mais recentes.")
    @GetMapping("/usage")
    public ResumoUso resumo() {
        return ResumoUso.de(repository.listar());
    }
}
