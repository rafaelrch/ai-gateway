package br.com.rafael.aigateway.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Strategy + Factory: o registry entrega o provedor certo para cada perfil.
 *
 * Teste de unidade puro: sem Spring. O registry é uma classe Java comum, e o
 * teste monta a lista de provedores na mão, como o Spring faria.
 */
class ProviderRegistryTest {

    @Test
    void entregaUmProvedorDiferenteParaCadaPerfil() {
        ProviderRegistry registry = new ProviderRegistry(List.of(
                new ProvedorFalso("FALSO_RAPIDO", Perfil.RAPIDO),
                new ProvedorFalso("FALSO_PREMIUM", Perfil.PREMIUM)), 3);

        // O nome atravessa os decorators: quem responde é o provedor certo.
        assertThat(registry.obter(Perfil.RAPIDO).nome()).isEqualTo("FALSO_RAPIDO");
        assertThat(registry.obter(Perfil.PREMIUM).nome()).isEqualTo("FALSO_PREMIUM");
        assertThat(registry.obter(Perfil.RAPIDO).gerar("oi").texto()).isEqualTo("FALSO_RAPIDO respondeu");
    }

    @Test
    void perfilSemProvedorLancaExcecaoPropria() {
        ProviderRegistry registry = new ProviderRegistry(
                List.of(new ProvedorFalso("FALSO_RAPIDO", Perfil.RAPIDO)), 3);

        assertThatThrownBy(() -> registry.obter(Perfil.ECONOMICO))
                .isInstanceOf(PerfilNaoSuportadoException.class)
                .hasMessageContaining("ECONOMICO");
    }

    @Test
    void doisProvedoresNoMesmoPerfilImpedemASubida() {
        assertThatThrownBy(() -> new ProviderRegistry(List.of(
                new ProvedorFalso("A", Perfil.RAPIDO),
                new ProvedorFalso("B", Perfil.RAPIDO)), 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("A")
                .hasMessageContaining("B");
    }

    @Test
    void oProvedorEntregueJaVemComRetry() {
        // Falha 2 vezes e responde na 3ª: só funciona se o registry embrulhou com retry.
        ProvedorFalso instavel = new ProvedorFalso("INSTAVEL", Perfil.ECONOMICO, 2);
        ProviderRegistry registry = new ProviderRegistry(List.of(instavel), 3);

        assertThat(registry.obter(Perfil.ECONOMICO).gerar("oi").texto()).isEqualTo("INSTAVEL respondeu");
        assertThat(instavel.chamadas()).isEqualTo(3);
    }
}
