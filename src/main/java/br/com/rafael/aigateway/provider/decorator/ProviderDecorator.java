package br.com.rafael.aigateway.provider.decorator;

import br.com.rafael.aigateway.provider.AiProvider;
import br.com.rafael.aigateway.provider.Perfil;

/**
 * PADRÃO: Decorator (a base de todos os decorators).
 *
 * Analogia: capa de celular. A capa tem o mesmo formato do celular (você
 * continua usando os mesmos botões), mas acrescenta algo por fora (proteção).
 * Dá para pôr capa em cima de película em cima do celular.
 *
 * No código: um decorator IMPLEMENTA AiProvider (tem o mesmo formato) e
 * GUARDA outro AiProvider dentro (o "envolvido"). Quem chama não sabe se está
 * falando com o provedor de verdade ou com uma camada por cima dele.
 *
 * Esta classe abstrata só repassa nome() e perfil() para o envolvido, que é
 * o que todo decorator faria igual. Cada decorator concreto sobrescreve
 * apenas gerar(), que é onde ele acrescenta comportamento.
 */
public abstract class ProviderDecorator implements AiProvider {

    // O provedor (ou outro decorator) que está "dentro" desta camada.
    // protected: as subclasses precisam chamar envolvido.gerar(...).
    protected final AiProvider envolvido;

    // Toda camada nasce já embrulhando alguém.
    protected ProviderDecorator(AiProvider envolvido) {
        this.envolvido = envolvido;
    }

    // O nome visto pelo cliente continua sendo o do provedor real.
    @Override
    public String nome() {
        return envolvido.nome();
    }

    // O perfil também: a capa não muda o modelo do celular.
    @Override
    public Perfil perfil() {
        return envolvido.perfil();
    }
}
