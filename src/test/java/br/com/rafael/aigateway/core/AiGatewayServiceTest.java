package br.com.rafael.aigateway.core;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import br.com.rafael.aigateway.core.chain.CacheHandler;
import br.com.rafael.aigateway.core.chain.ConteudoHandler;
import br.com.rafael.aigateway.core.chain.CorrenteDeValidacao;
import br.com.rafael.aigateway.core.chain.TamanhoHandler;
import br.com.rafael.aigateway.provider.Perfil;
import br.com.rafael.aigateway.provider.ProvedorFalso;
import br.com.rafael.aigateway.provider.ProviderRegistry;
import br.com.rafael.aigateway.usage.UsoRegistradoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Facade + Chain + Observer, testando o service de fora para dentro.
 *
 * Peças reais: corrente, cache e registry com um provedor falso.
 * Peça falsa (mock do Mockito): o publicador de eventos, para conferir o que
 * foi publicado sem precisar subir o Spring.
 */
class AiGatewayServiceTest {

    private ProvedorFalso provedor;
    private ProviderRegistry registry;
    private ApplicationEventPublisher eventos;
    private AiGatewayService service;

    @BeforeEach
    void montar() {
        provedor = new ProvedorFalso("FALSO_RAPIDO", Perfil.RAPIDO);
        // spy: objeto real, mas o Mockito registra as chamadas para verify().
        registry = spy(new ProviderRegistry(List.of(provedor), 3));
        eventos = mock(ApplicationEventPublisher.class);
        CacheDeRespostas cache = new CacheDeRespostas();
        CorrenteDeValidacao corrente = new CorrenteDeValidacao(List.of(
                new TamanhoHandler(100),
                new ConteudoHandler(List.of("senha")),
                new CacheHandler(cache)));
        service = new AiGatewayService(corrente, registry, cache, eventos);
    }

    @Test
    void promptBarradoNaoChegaNoProvedorNemGeraEvento() {
        CompletionRequest longo = new CompletionRequest("a".repeat(1000), Perfil.RAPIDO);

        assertThatThrownBy(() -> service.processar(longo)).isInstanceOf(PromptBloqueadoException.class);

        // O ponto do Chain: barrou ANTES de escolher provedor.
        verify(registry, never()).obter(any());
        assertThat(provedor.chamadas()).isZero();
        verifyNoInteractions(eventos);
    }

    @Test
    void caminhoFelizChamaOProvedorEPublicaUmEvento() {
        CompletionResponse resposta = service.processar(new CompletionRequest("oi", Perfil.RAPIDO));

        assertThat(resposta.provedorUsado()).isEqualTo("FALSO_RAPIDO");
        assertThat(resposta.cacheHit()).isFalse();
        assertThat(resposta.id()).isNotBlank();

        // Observer: o service publicou exatamente um evento, com os dados da resposta.
        ArgumentCaptor<UsoRegistradoEvent> evento = ArgumentCaptor.forClass(UsoRegistradoEvent.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().requisicaoId()).isEqualTo(resposta.id());
        assertThat(evento.getValue().tokens()).isEqualTo(10);
    }

    @Test
    void segundaChamadaIgualVemDoCacheSemCustoESemProvedor() {
        CompletionRequest req = new CompletionRequest("oi", Perfil.RAPIDO);

        CompletionResponse primeira = service.processar(req);
        CompletionResponse segunda = service.processar(req);

        assertThat(segunda.cacheHit()).isTrue();
        assertThat(segunda.custoEstimado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(segunda.tokensGastos()).isZero();
        assertThat(segunda.resposta()).isEqualTo(primeira.resposta());
        assertThat(segunda.id()).isNotEqualTo(primeira.id());
        // O provedor só foi chamado uma vez, mas houve dois eventos de uso.
        assertThat(provedor.chamadas()).isEqualTo(1);
        verify(eventos, times(2)).publishEvent(any(UsoRegistradoEvent.class));
    }
}
