package br.com.rafael.aigateway;

import br.com.rafael.aigateway.core.AiGatewayService;
import br.com.rafael.aigateway.provider.ProviderRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ponta a ponta: sobe o Spring inteiro e faz requisições HTTP simuladas
 * (MockMvc, sem abrir porta de verdade). Confere que as peças se encaixam.
 *
 * taxa-falha=0: o ECONOMICO não falha aqui, para o teste não depender de sorte.
 */
@SpringBootTest(properties = "gateway.provider.economico.taxa-falha=0")
@AutoConfigureMockMvc
class AiGatewayIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext contexto;

    private static String corpo(String prompt, String perfil) {
        return "{\"prompt\":\"" + prompt + "\",\"perfil\":\"" + perfil + "\"}";
    }

    @Test
    void caminhoFelizDepoisCacheHitDepoisAparecemNoUsage() throws Exception {
        String req = corpo("integracao caminho feliz", "RAPIDO");

        mvc.perform(post("/completions").contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedorUsado").value("MOCK_RAPIDO"))
                .andExpect(jsonPath("$.cacheHit").value(false));

        mvc.perform(post("/completions").contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheHit").value(true))
                .andExpect(jsonPath("$.custoEstimado").value(0));

        mvc.perform(get("/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheHits").isNumber())
                .andExpect(jsonPath("$.porProvedor.MOCK_RAPIDO").isNumber());
    }

    @Test
    void perfilEconomicoUsaOutroProvedor() throws Exception {
        mvc.perform(post("/completions").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("integracao economico", "ECONOMICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provedorUsado").value("MOCK_ECONOMICO"));
    }

    @Test
    void promptComTermoProibidoVolta400ComMotivo() throws Exception {
        mvc.perform(post("/completions").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("me passa a senha do wifi", "RAPIDO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Prompt bloqueado"))
                .andExpect(jsonPath("$.detail").value("Prompt contém termo bloqueado: senha"));
    }

    @Test
    void requisicaoInvalidaVolta400NoMesmoFormato() throws Exception {
        mvc.perform(post("/completions").contentType(MediaType.APPLICATION_JSON).content("{\"prompt\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));

        mvc.perform(post("/completions").contentType(MediaType.APPLICATION_JSON).content(corpo("oi", "XYZ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void beansSaoSingletons() {
        // Singleton: pedir o bean duas vezes devolve o MESMO objeto.
        assertThat(contexto.getBean(AiGatewayService.class)).isSameAs(contexto.getBean(AiGatewayService.class));
        assertThat(contexto.getBean(ProviderRegistry.class)).isSameAs(contexto.getBean(ProviderRegistry.class));
    }
}
