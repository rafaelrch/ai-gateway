package br.com.rafael.aigateway.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * PADRÃO: Observer (um observador).
 *
 * Ouve o UsoRegistradoEvent e grava no repositório. O AiGatewayService não
 * sabe que esta classe existe: ele só publica o evento.
 *
 * Síncrono: por padrão o Spring chama o listener na MESMA thread que
 * publicou, antes de publishEvent() retornar. Se este método demorasse, a
 * resposta ao cliente demoraria junto. Para rodar em outra thread, bastaria
 * @Async aqui e @EnableAsync na aplicação (ver docs/PADROES.md).
 */
@Component
public class UsageListener {

    private static final Logger log = LoggerFactory.getLogger(UsageListener.class);

    // Onde o uso fica guardado.
    private final UsageRepository repository;

    public UsageListener(UsageRepository repository) {
        this.repository = repository;
    }

    // @EventListener: o Spring olha o tipo do parâmetro e chama este método
    // toda vez que alguém publicar um UsoRegistradoEvent.
    @EventListener
    public void aoRegistrarUso(UsoRegistradoEvent evento) {
        repository.salvar(evento);
        log.info("[observer] uso registrado: {} tokens, custo {}, cacheHit {}",
                evento.tokens(), evento.custo(), evento.cacheHit());
    }
}
