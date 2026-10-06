package br.com.rafael.aigateway.core.chain;

import br.com.rafael.aigateway.core.PromptBloqueadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Elo 2 da corrente: barra prompt com termo proibido.
 *
 * Evita mandar dado sensível (senha, CPF, cartão) para um provedor externo.
 * A comparação ignora maiúscula e acento: "CPF", "cpf" e "Cartão de Crédito"
 * são todos pegos.
 */
@Component
@Order(2)   // roda depois do TamanhoHandler
public class ConteudoHandler implements PromptHandler {

    private static final Logger log = LoggerFactory.getLogger(ConteudoHandler.class);

    // Lista de termos já normalizados (minúsculos e sem acento).
    private final List<String> termosProibidos;

    // O Spring lê a propriedade separada por vírgula e entrega como List<String>.
    public ConteudoHandler(@Value("${gateway.chain.termos-proibidos}") List<String> termos) {
        // Normaliza cada termo uma vez só, na subida, em vez de a cada requisição.
        this.termosProibidos = termos.stream()
                .map(ConteudoHandler::normalizar)
                .toList(); // toList() devolve lista imutável
    }

    @Override
    public void handle(ContextoRequisicao ctx) {
        // Normaliza o prompt do mesmo jeito que os termos, para comparar igual com igual.
        String prompt = normalizar(ctx.requisicao().prompt());

        // Procura cada termo dentro do prompt.
        for (String termo : termosProibidos) {
            if (prompt.contains(termo)) {
                // Achou: barra. A mensagem diz o motivo para o cliente corrigir.
                log.info("[chain] ConteudoHandler: termo '{}' encontrado, barrado", termo);
                throw new PromptBloqueadoException("Prompt contém termo bloqueado: " + termo);
            }
        }

        // Nenhum termo achado: segue.
        log.info("[chain] ConteudoHandler: nenhum termo bloqueado");
    }

    // Tira acento e passa para minúsculo. "Cartão" vira "cartao".
    static String normalizar(String texto) {
        // NFD separa a letra do acento ("ã" vira "a" + "~").
        String decomposto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        // \p{M} casa as marcas de acento soltas. Apaga todas e baixa a caixa.
        return decomposto.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
    }
}
