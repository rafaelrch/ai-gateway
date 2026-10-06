package br.com.rafael.aigateway.provider.decorator;

import br.com.rafael.aigateway.provider.AiProvider;
import br.com.rafael.aigateway.provider.Perfil;
import br.com.rafael.aigateway.provider.ProvedorFalso;
import br.com.rafael.aigateway.provider.ProvedorIndisponivelException;
import br.com.rafael.aigateway.provider.RespostaIa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Decorator: o retry tenta de novo sem o provedor saber.
 */
class RetryProviderDecoratorTest {

    @Test
    void tentaDeNovoAteOProvedorResponder() {
        // Falha nas 2 primeiras chamadas, responde na 3ª.
        ProvedorFalso provedor = new ProvedorFalso("INSTAVEL", Perfil.ECONOMICO, 2);
        AiProvider comRetry = new RetryProviderDecorator(provedor, 3);

        RespostaIa resposta = comRetry.gerar("oi");

        assertThat(resposta.texto()).isEqualTo("INSTAVEL respondeu");
        assertThat(provedor.chamadas()).isEqualTo(3);
    }

    @Test
    void desisteDepoisDoMaximoDeTentativas() {
        // Falharia 5 vezes, mas o retry só deixa tentar 3.
        ProvedorFalso provedor = new ProvedorFalso("FORA_DO_AR", Perfil.ECONOMICO, 5);
        AiProvider comRetry = new RetryProviderDecorator(provedor, 3);

        assertThatThrownBy(() -> comRetry.gerar("oi"))
                .isInstanceOf(ProvedorIndisponivelException.class);
        assertThat(provedor.chamadas()).isEqualTo(3);
    }

    @Test
    void naoRepeteErroQueNaoETemporario() {
        // Provedor com bug: lança outra exceção. Repetir não adianta, então não repete.
        int[] chamadas = {0};
        AiProvider comBug = new ProvedorFalso("BUGADO", Perfil.RAPIDO) {
            @Override
            public RespostaIa gerar(String prompt) {
                chamadas[0]++;
                throw new IllegalArgumentException("bug");
            }
        };

        assertThatThrownBy(() -> new RetryProviderDecorator(comBug, 3).gerar("oi"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(chamadas[0]).isEqualTo(1);
    }

    @Test
    void decoratorRepassaNomeEPerfilDoEnvolvido() {
        AiProvider empilhado = new LoggingProviderDecorator(
                new RetryProviderDecorator(new ProvedorFalso("REAL", Perfil.PREMIUM), 3));

        assertThat(empilhado.nome()).isEqualTo("REAL");
        assertThat(empilhado.perfil()).isEqualTo(Perfil.PREMIUM);
    }
}
