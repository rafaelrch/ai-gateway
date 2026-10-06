package br.com.rafael.aigateway.provider;

import br.com.rafael.aigateway.provider.decorator.LoggingProviderDecorator;
import br.com.rafael.aigateway.provider.decorator.RetryProviderDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * PADRÃO: Factory (na forma de registry) + montagem dos Decorators.
 *
 * Analogia: o quadro de chaves da portaria. Na subida do prédio (da app), o
 * porteiro pendura cada chave no gancho do seu apartamento. Depois, quem
 * chega diz o número do apartamento (o perfil) e recebe a chave certa, sem
 * precisar procurar nem saber como a chave foi feita.
 *
 * Quem decide QUAL provedor atende cada perfil é esta classe, e não o
 * service. Sem ela, o if/else de perfil voltaria para o service.
 *
 * Honestidade de nomenclatura: quem CRIA os provedores é o container do
 * Spring. Esta classe recebe os prontos, organiza num mapa e devolve sob
 * demanda. O nome mais preciso é registry. O que ela de fato fabrica são as
 * camadas de decorator em volta de cada provedor.
 */
@Component
public class ProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProviderRegistry.class);

    // O quadro de chaves: perfil -> provedor já decorado.
    // EnumMap é um mapa otimizado para chave enum (por dentro é um array).
    private final Map<Perfil, AiProvider> provedores = new EnumMap<>(Perfil.class);

    /**
     * Roda uma vez, na subida da aplicação.
     *
     * @param providerList  todos os beans que implementam AiProvider. O Spring
     *                      acha cada @Component e entrega a lista pronta.
     * @param maxTentativas quantas tentativas o retry faz (application.properties).
     */
    public ProviderRegistry(List<AiProvider> providerList,
                            @Value("${gateway.provider.max-tentativas:3}") int maxTentativas) {

        // Percorre cada provedor encontrado pelo Spring.
        for (AiProvider provedor : providerList) {

            // Empilha as camadas: log por fora, retry no meio, provedor no centro.
            AiProvider decorado = new LoggingProviderDecorator(
                    new RetryProviderDecorator(provedor, maxTentativas));

            // put devolve o valor que estava antes nesse gancho (ou null se vazio).
            AiProvider anterior = provedores.put(provedor.perfil(), decorado);

            // Já tinha alguém no mesmo perfil: configuração errada. A app nem sobe,
            // porque é melhor quebrar na subida do que escolher um ao acaso em produção.
            if (anterior != null) {
                throw new IllegalStateException(
                        "Dois provedores para o perfil " + provedor.perfil()
                                + ": " + anterior.nome() + " e " + provedor.nome());
            }
        }

        // Mostra no console o mapa final, para conferir na subida.
        log.info("[factory] provedores registrados: {}", provedores.keySet());
    }

    // Entrega o provedor do perfil pedido. É chamado a cada requisição.
    public AiProvider obter(Perfil perfil) {
        // Procura no mapa. null = perfil sem provedor.
        AiProvider provedor = provedores.get(perfil);

        // Perfil existe no enum mas ninguém foi registrado para ele: erro 400.
        if (provedor == null) {
            throw new PerfilNaoSuportadoException(perfil);
        }

        log.info("[strategy] perfil {} resolvido para {}", perfil, provedor.nome());
        return provedor;
    }
}
