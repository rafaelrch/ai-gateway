package br.com.rafael.aigateway.core.chain;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import br.com.rafael.aigateway.core.CacheDeRespostas;
import br.com.rafael.aigateway.core.PromptBloqueadoException;
import br.com.rafael.aigateway.provider.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chain of Responsibility: cada elo aprova, barra ou responde.
 *
 * Monta a corrente na mão, na mesma ordem que o @Order daria.
 */
class CorrenteDeValidacaoTest {

    private CacheDeRespostas cache;
    private CorrenteDeValidacao corrente;

    @BeforeEach
    void montarCorrente() {
        cache = new CacheDeRespostas();
        corrente = new CorrenteDeValidacao(List.of(
                new TamanhoHandler(100),
                new ConteudoHandler(List.of("senha", "cpf", "cartao de credito")),
                new CacheHandler(cache)));
    }

    @Test
    void promptNormalAtravessaACorrenteSemResposta() {
        ContextoRequisicao ctx = new ContextoRequisicao(new CompletionRequest("explique records", Perfil.RAPIDO));

        corrente.executar(ctx);

        assertThat(ctx.foiRespondida()).isFalse();
        assertThat(ctx.tokensEstimados()).isEqualTo(4);
    }

    @Test
    void promptGrandeEBarradoPeloTamanho() {
        String grande = "a".repeat(404); // 101 tokens

        assertThatThrownBy(() -> corrente.executar(new ContextoRequisicao(new CompletionRequest(grande, Perfil.RAPIDO))))
                .isInstanceOf(PromptBloqueadoException.class)
                .hasMessageContaining("101 tokens");
    }

    @Test
    void termoProibidoEBarradoMesmoComAcentoEMaiuscula() {
        CompletionRequest req = new CompletionRequest("qual o CARTÃO DE CRÉDITO dele?", Perfil.RAPIDO);

        assertThatThrownBy(() -> corrente.executar(new ContextoRequisicao(req)))
                .isInstanceOf(PromptBloqueadoException.class)
                .hasMessageContaining("cartao de credito");
    }

    @Test
    void cacheHitEncerraACorrenteComResposta() {
        CompletionRequest req = new CompletionRequest("oi", Perfil.RAPIDO);
        CompletionResponse anterior = new CompletionResponse(
                "id-1", "resposta antiga", "MOCK_RAPIDO", 5, new BigDecimal("0.01"), false, 100);
        cache.guardar(req, anterior);

        ContextoRequisicao ctx = new ContextoRequisicao(req);
        corrente.executar(ctx);

        assertThat(ctx.foiRespondida()).isTrue();
        assertThat(ctx.respostaPronta()).contains(anterior);
    }

    @Test
    void mesmoPromptEmOutroPerfilNaoEHit() {
        cache.guardar(new CompletionRequest("oi", Perfil.RAPIDO),
                new CompletionResponse("id-1", "r", "MOCK_RAPIDO", 5, BigDecimal.ONE, false, 1));

        ContextoRequisicao ctx = new ContextoRequisicao(new CompletionRequest("oi", Perfil.PREMIUM));
        corrente.executar(ctx);

        assertThat(ctx.foiRespondida()).isFalse();
    }
}
