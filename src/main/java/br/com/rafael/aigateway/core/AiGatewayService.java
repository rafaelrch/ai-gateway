package br.com.rafael.aigateway.core;

import br.com.rafael.aigateway.api.dto.CompletionRequest;
import br.com.rafael.aigateway.api.dto.CompletionResponse;
import br.com.rafael.aigateway.core.chain.ContextoRequisicao;
import br.com.rafael.aigateway.core.chain.CorrenteDeValidacao;
import br.com.rafael.aigateway.provider.AiProvider;
import br.com.rafael.aigateway.provider.ProviderRegistry;
import br.com.rafael.aigateway.provider.RespostaIa;
import br.com.rafael.aigateway.usage.UsoRegistradoEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * PADRÃO: Facade (a fachada do gateway) e Singleton (bean de escopo único).
 *
 * FACADE. Analogia: o balcão do restaurante. O cliente faz o pedido num
 * lugar só e não fala com cozinha, estoque e caixa separadamente. O
 * controller é o cliente: chama processar() e pronto. Por trás, esta classe
 * coordena quatro subsistemas, cada um com o seu padrão:
 *   1. CorrenteDeValidacao   (Chain of Responsibility) barra ou responde do cache
 *   2. ProviderRegistry      (Factory/registry + Strategy) escolhe o provedor
 *      (já embrulhado pelos Decorators de retry e log)
 *   3. CacheDeRespostas      guarda a resposta para a próxima vez
 *   4. ApplicationEventPublisher (Observer) avisa que houve uso
 *
 * Note que processar() não tem nenhuma regra de negócio dentro: nenhum if de
 * perfil, de tamanho, de termo ou de retry. Só a ordem dos passos.
 *
 * SINGLETON. @Service cria UM objeto desta classe para a aplicação inteira,
 * compartilhado por todas as requisições ao mesmo tempo. Por isso os campos
 * são todos final e apontam para outros beans sem estado por requisição. O
 * que muda a cada requisição (contexto, tempos, resposta) vive em variável
 * local, que é exclusiva de cada thread. Um campo como "private int
 * tokensDaRequisicaoAtual" aqui seria um bug de concorrência: duas
 * requisições sobrescreveriam o valor uma da outra.
 */
@Service
public class AiGatewayService {

    // Subsistema 1: as regras que o prompt atravessa.
    private final CorrenteDeValidacao corrente;
    // Subsistema 2: o "quadro de chaves" perfil -> provedor.
    private final ProviderRegistry registry;
    // Subsistema 3: respostas já geradas.
    private final CacheDeRespostas cache;
    // Subsistema 4: o "sino" do Observer. O Spring injeta o próprio contexto aqui.
    private final ApplicationEventPublisher eventos;

    // Injeção por construtor: o Spring vê os quatro parâmetros e entrega os beans.
    // Com um construtor só, nem precisa de @Autowired.
    public AiGatewayService(CorrenteDeValidacao corrente,
                            ProviderRegistry registry,
                            CacheDeRespostas cache,
                            ApplicationEventPublisher eventos) {
        this.corrente = corrente;
        this.registry = registry;
        this.cache = cache;
        this.eventos = eventos;
    }

    /**
     * O único método público da fachada: recebe o pedido e devolve a resposta.
     * Lê como uma receita, de cima para baixo.
     */
    public CompletionResponse processar(CompletionRequest requisicao) {
        // Marca o início para calcular a duração total no fim.
        long inicio = System.currentTimeMillis();

        // Passo 1: cria o envelope desta requisição e passa pela corrente.
        // Se um handler barrar, a PromptBloqueadoException sai daqui e vira 400.
        ContextoRequisicao ctx = new ContextoRequisicao(requisicao);
        corrente.executar(ctx);

        // Passo 2: se o CacheHandler respondeu, devolve sem chamar provedor.
        Optional<CompletionResponse> doCache = ctx.respostaPronta();
        if (doCache.isPresent()) {
            CompletionResponse resposta = respostaDoCache(doCache.get(), inicio);
            publicarUso(requisicao, resposta);
            return resposta;
        }

        // Passo 3: pede ao registry o provedor do perfil (já decorado com retry e log).
        AiProvider provedor = registry.obter(requisicao.perfil());
        // Passo 4: gera. O service não sabe qual mock é, nem que existe retry por dentro.
        RespostaIa gerada = provedor.gerar(requisicao.prompt());

        // Passo 5: monta a resposta da API, guarda no cache e avisa os observadores.
        CompletionResponse resposta = respostaNova(provedor, gerada, inicio);
        cache.guardar(requisicao, resposta);
        publicarUso(requisicao, resposta);

        return resposta;
    }

    // Monta a resposta de um cache hit: mesmo texto, mas custo e tokens zerados,
    // porque nenhum provedor foi chamado desta vez. Id novo: é outra requisição.
    private CompletionResponse respostaDoCache(CompletionResponse original, long inicio) {
        return new CompletionResponse(
                novoId(),
                original.resposta(),
                original.provedorUsado(),
                0,
                BigDecimal.ZERO,
                true,
                System.currentTimeMillis() - inicio);
    }

    // Monta a resposta com o que o provedor devolveu mais o que só o gateway sabe
    // (id, nome do provedor, se veio do cache, duração).
    private CompletionResponse respostaNova(AiProvider provedor, RespostaIa gerada, long inicio) {
        return new CompletionResponse(
                novoId(),
                gerada.texto(),
                provedor.nome(),
                gerada.tokens(),
                gerada.custo(),
                false,
                System.currentTimeMillis() - inicio);
    }

    // Toca o sino: publica o evento. Quem ouve (UsageListener, AlertaDeCustoListener)
    // é problema do Spring. O service não conhece nenhum deles.
    private void publicarUso(CompletionRequest requisicao, CompletionResponse resposta) {
        eventos.publishEvent(new UsoRegistradoEvent(
                resposta.id(),
                requisicao.perfil(),
                resposta.provedorUsado(),
                resposta.tokensGastos(),
                resposta.custoEstimado(),
                resposta.cacheHit(),
                resposta.duracaoMs(),
                Instant.now()));
    }

    // Id único por requisição. UUID aleatório: chance de repetir é desprezível.
    private String novoId() {
        return UUID.randomUUID().toString();
    }
}
