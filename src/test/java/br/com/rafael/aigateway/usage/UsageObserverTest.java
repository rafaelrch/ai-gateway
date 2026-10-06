package br.com.rafael.aigateway.usage;

import br.com.rafael.aigateway.provider.Perfil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Observer do lado de quem ouve: o listener grava e o resumo soma certo.
 */
class UsageObserverTest {

    private UsoRegistradoEvent evento(String provedor, int tokens, String custo, boolean cacheHit) {
        return new UsoRegistradoEvent("id", Perfil.RAPIDO, provedor, tokens,
                new BigDecimal(custo), cacheHit, 10, Instant.now());
    }

    @Test
    void listenerGravaNoRepositorioEOResumoAgrega() {
        UsageRepository repository = new UsageRepository();
        UsageListener listener = new UsageListener(repository);

        listener.aoRegistrarUso(evento("MOCK_RAPIDO", 10, "0.001", false));
        listener.aoRegistrarUso(evento("MOCK_RAPIDO", 0, "0", true));
        listener.aoRegistrarUso(evento("MOCK_PREMIUM", 50, "0.010", false));

        ResumoUso resumo = ResumoUso.de(repository.listar());

        assertThat(resumo.totalRequisicoes()).isEqualTo(3);
        assertThat(resumo.cacheHits()).isEqualTo(1);
        assertThat(resumo.totalTokens()).isEqualTo(60);
        assertThat(resumo.custoTotal()).isEqualByComparingTo("0.011");
        assertThat(resumo.porProvedor()).containsEntry("MOCK_RAPIDO", 2L).containsEntry("MOCK_PREMIUM", 1L);
        // A mais recente vem primeiro.
        assertThat(resumo.ultimas().getFirst().provedor()).isEqualTo("MOCK_PREMIUM");
    }

    @Test
    void resumoDeHistoricoVazioEZero() {
        ResumoUso resumo = ResumoUso.de(java.util.List.of());

        assertThat(resumo.totalRequisicoes()).isZero();
        assertThat(resumo.custoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumo.ultimas()).isEmpty();
    }
}
