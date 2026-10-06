package br.com.rafael.aigateway.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PADRÃO: Observer (um segundo observador do mesmo evento).
 *
 * Existe para mostrar a vantagem do Observer: uma reação nova ao mesmo
 * evento, sem mexer no AiGatewayService nem no UsageListener. Avisa no log
 * quando uma requisição custa mais que o limite configurado.
 */
@Component
public class AlertaDeCustoListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaDeCustoListener.class);

    // Acima deste custo (em reais) por requisição, gera alerta.
    private final BigDecimal limite;

    // O texto da propriedade vira BigDecimal pela conversão do Spring.
    public AlertaDeCustoListener(@Value("${gateway.usage.alerta-custo:0.01}") BigDecimal limite) {
        this.limite = limite;
    }

    @EventListener
    public void aoRegistrarUso(UsoRegistradoEvent evento) {
        // compareTo devolve > 0 quando custo é maior que limite. Com BigDecimal
        // não se usa ">" nem equals (equals compara também a escala: 0.10 != 0.1).
        if (evento.custo().compareTo(limite) > 0) {
            log.warn("[observer] alerta de custo: requisição {} custou {} (limite {})",
                    evento.requisicaoId(), evento.custo(), limite);
        }
    }
}
