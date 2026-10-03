package br.com.rafael.aigateway.provider;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderRegistry {

    private final Map<Perfil, AiProvider> provedores;

    public ProviderRegistry(List<AiProvider> providerList) {
        this.provedores = new HashMap<>();

        for (AiProvider aiProvider : providerList) {
            AiProvider anterior = provedores.put(aiProvider.perfil(), aiProvider);

            if (anterior != null) {
                throw new IllegalStateException(
                        "Dois provedores para o perfil " + aiProvider.perfil()
                                + ": " + anterior.nome() + " e " + aiProvider.nome());
            }
        }
    }

    public AiProvider obter(Perfil perfil) {
        AiProvider provedor = provedores.get(perfil);

        if (provedor == null) {
            throw new PerfilNaoSuportadoException(perfil);
        }

        return provedor;
    }
}
