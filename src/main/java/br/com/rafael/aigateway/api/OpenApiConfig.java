package br.com.rafael.aigateway.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Texto que aparece no topo do Swagger UI (http://localhost:8080/swagger-ui.html).
 *
 * O springdoc lê os controllers sozinho e gera a documentação dos endpoints.
 * Este bean só acrescenta título e descrição.
 */
@Configuration
public class OpenApiConfig {

    // @Bean: o objeto devolvido vira um bean, e o springdoc o usa como base.
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("ai-gateway")
                .version("1.0.0")
                .description("""
                        Gateway entre uma aplicação cliente e provedores de IA (simulados).
                        Perfis: RAPIDO, PREMIUM e ECONOMICO (instável, com retry).
                        Prompt com mais de 100 tokens ou com termo proibido volta 400.
                        O mesmo prompt no mesmo perfil volta do cache, com custo zero.
                        Padrões: Strategy, Factory, Chain of Responsibility, Decorator,
                        Observer, Facade e Singleton. Detalhes em docs/PADROES.md."""));
    }
}
