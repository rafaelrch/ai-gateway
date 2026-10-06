# Padrões de projeto no ai-gateway

Guia de estudo. Para cada padrão: o que é, a dor que ele resolve aqui, como foi aplicado (com os arquivos), o que o Spring faz por baixo, variações, armadilhas e onde o próprio Spring usa o padrão.

Todo arquivo `.java` do projeto tem comentário de bloco no topo dizendo qual padrão ele implementa e comentários linha a linha. Este documento é o mapa. O código é o território.

**Ordem sugerida de estudo:** programar para interface → Strategy → Factory → Chain of Responsibility → Decorator → Observer → Facade → Singleton. É a mesma ordem em que os padrões entraram no código (ver o histórico de commits no README).

---

## Sumário

0. [A base: programar para interface](#0-a-base-programar-para-interface)
1. [Strategy](#1-strategy)
2. [Factory (registry)](#2-factory-registry)
3. [Chain of Responsibility](#3-chain-of-responsibility)
4. [Decorator](#4-decorator)
5. [Observer](#5-observer)
6. [Facade](#6-facade)
7. [Singleton](#7-singleton)
8. [Uma requisição, arquivo por arquivo](#8-uma-requisição-arquivo-por-arquivo)
9. [Os testes de cada padrão](#9-os-testes-de-cada-padrão)
10. [Perguntas de entrevista](#10-perguntas-de-entrevista)

---

## 0. A base: programar para interface

Quase todos os padrões daqui dependem de uma ideia: **quem usa um objeto conhece só o contrato dele (a interface), nunca a classe concreta.**

Analogia: a tomada. O secador não sabe se a energia vem de hidrelétrica ou de painel solar. Ele conhece o formato da tomada. Troca a usina, o secador continua funcionando.

No projeto, o `AiGatewayService` conhece `AiProvider` (a tomada) e nunca `MockRapidoProvider` (a usina). É isso que permite:

- trocar o provedor de um perfil sem mexer no service (Strategy);
- pôr camadas em volta de um provedor sem ele saber (Decorator);
- testar o service com um provedor falso (`ProvedorFalso`, nos testes).

**Princípio aberto/fechado** (o "O" do SOLID): o código fica *aberto para extensão* (dá para acrescentar provedor, regra, observador) e *fechado para modificação* (sem abrir os arquivos que já funcionam). Cada padrão abaixo é uma forma de conseguir isso num ponto diferente do sistema.

---

## 1. Strategy

### O que é

Separar **vários jeitos de fazer a mesma coisa** em classes diferentes que seguem a mesma interface, para que quem usa possa trocar um pelo outro sem mudar o próprio código.

Analogia: furadeira e brocas. A furadeira (quem usa) tem um mandril (a interface). Cada broca (estratégia) fura de um jeito: madeira, concreto, metal. Trocar a broca não muda a furadeira.

### A dor no projeto

Versão feia (commit `5cf3d80`):

```java
if(completionRequest.perfil().equals("RAPIDO")){
    Thread.sleep(100);
    resposta = "teste para RAPIDO";
    provedorUsado = "MOCK_RAPIDO";
    custoEstimado = new BigDecimal("0.12");
    horaFim = System.currentTimeMillis();
} else if (completionRequest.perfil().equals("PREMIUM")){
    Thread.sleep(800);
    resposta = "teste para PREMIUM";
    provedorUsado = "MOCK_PREMIUM";
    custoEstimado = new BigDecimal("2.12");
    horaFim = System.currentTimeMillis();
}else{
    throw new IllegalArgumentException("Perfil Desconhecido");
}
```

Para acrescentar um terceiro provedor, era preciso abrir o service de novo, mexer no método que atende todos os perfis e testar todos outra vez.

### Como foi aplicado

| Peça do padrão | No projeto |
|---|---|
| Interface da estratégia | [`AiProvider`](../src/main/java/br/com/rafael/aigateway/provider/AiProvider.java): `gerar(prompt)`, `nome()`, `perfil()` |
| Estratégias concretas | [`MockRapidoProvider`](../src/main/java/br/com/rafael/aigateway/provider/MockRapidoProvider.java), [`MockPremiumProvider`](../src/main/java/br/com/rafael/aigateway/provider/MockPremiumProvider.java), [`MockEconomicoProvider`](../src/main/java/br/com/rafael/aigateway/provider/MockEconomicoProvider.java) |
| Contexto (quem usa) | [`AiGatewayService`](../src/main/java/br/com/rafael/aigateway/core/AiGatewayService.java), que chama `provedor.gerar(...)` sem saber qual é |
| Quem escolhe | [`ProviderRegistry`](../src/main/java/br/com/rafael/aigateway/provider/ProviderRegistry.java) (ver Factory) |

A prova de que funcionou: o `MockEconomicoProvider` e o perfil `ECONOMICO` entraram no Módulo 4 **sem mudar uma linha do `AiGatewayService`**.

### O que o Spring faz por baixo

Cada provedor tem `@Component`. Na subida, o *component scan* acha as três classes, cria um bean de cada e, quando algum construtor pede `List<AiProvider>`, o Spring entrega a lista com todos os beans que implementam a interface. Ninguém registra provedor na mão.

### Teoria

- **O Strategy separa os jeitos; ele não escolhe.** A escolha fica com o cliente da estratégia. No laboratório da DIO, o `Test.main` escolhe e chama `robo.setComportamento(...)`. Aqui, a escolha foi para o `ProviderRegistry`.
- **Setter x construtor.** O `Robo` do lab troca de estratégia com setter. Isso funciona num objeto usado por uma pessoa só. Num bean do Spring, que é compartilhado por todas as requisições ao mesmo tempo, trocar a estratégia por setter seria **condição de corrida**: a requisição A põe RAPIDO, a B põe PREMIUM, e a A acaba usando o PREMIUM. Por isso aqui a estratégia é escolhida por requisição e vive em variável local.
- **Strategy x if/else.** Strategy vale quando os jeitos têm lógica própria e tendem a crescer. Para dois casos de uma linha cada, um `if` é mais honesto.

### Onde o Spring usa

- `HttpMessageConverter`: um para JSON, um para XML, um para texto. O Spring escolhe pelo `Content-Type`.
- `PasswordEncoder` no Spring Security: BCrypt, Argon2, PBKDF2, todos atrás da mesma interface.
- `Comparator` do Java é Strategy: `lista.sort(comparadorPorNome)`.

---

## 2. Factory (registry)

### O que é

Tirar de quem usa a responsabilidade de **decidir e montar** qual objeto concreto criar. Quem usa pede pelo que precisa ("o provedor do perfil RAPIDO") e recebe pronto.

Analogia: o quadro de chaves da portaria. Na abertura do prédio, o porteiro pendura cada chave no gancho do seu apartamento. Depois, quem chega diz o número e recebe a chave certa, sem procurar nem saber como ela foi feita.

### A dor no projeto

Depois do Strategy (commit `4bec2f5`), o service ainda precisava escolher:

```java
AiProvider providerEscolhido = null;
for(AiProvider provedor : provedores){
    if(provedor.nome().equals("MOCK_" + completionRequest.perfil())){
        providerEscolhido = provedor;
        break;
    }
}
```

Isso é o `if` de perfil voltando pela porta dos fundos, e ainda dependia de convenção de nome em texto.

### Como foi aplicado

[`ProviderRegistry`](../src/main/java/br/com/rafael/aigateway/provider/ProviderRegistry.java):

1. No construtor (uma vez, na subida), recebe `List<AiProvider>` do Spring.
2. Para cada provedor, **fabrica as camadas de decorator** em volta dele (ver Decorator) e pendura no `EnumMap<Perfil, AiProvider>`.
3. Se dois provedores declaram o mesmo perfil, lança `IllegalStateException`, e **a aplicação nem sobe**. É melhor quebrar na subida que escolher um ao acaso em produção.
4. `obter(perfil)` devolve o provedor ou lança [`PerfilNaoSuportadoException`](../src/main/java/br/com/rafael/aigateway/provider/PerfilNaoSuportadoException.java) (vira 400).

O perfil chega como enum [`Perfil`](../src/main/java/br/com/rafael/aigateway/provider/Perfil.java). O Jackson converte o texto do JSON no enum e recusa valores que não existem antes de chegar no controller.

### Honestidade de nomenclatura

Quem **cria** os provedores é o container do Spring. O registry recebe os prontos e organiza num mapa. O nome mais preciso para isso é **registry** (catálogo de objetos já criados). O que o registry de fato fabrica são as **camadas de decorator** em volta de cada provedor. Por isso ele é chamado aqui de "Factory na forma de registry".

### Teoria: as três "factories"

| Variação | O que é | Exemplo |
|---|---|---|
| **Simple Factory** | Um método com `switch`/mapa que devolve o objeto certo. Não é um padrão do livro do GoF, é um idioma | `ProviderRegistry.obter(perfil)` |
| **Factory Method** (GoF) | Uma classe base declara um método abstrato `criar()`, e cada subclasse decide o que criar | `Collection.iterator()`: cada coleção cria o seu iterador |
| **Abstract Factory** (GoF) | Uma interface que cria uma **família** de objetos relacionados | Uma fábrica de tema que cria botão, menu e janela do mesmo estilo |

### Onde o Spring usa

- O próprio `ApplicationContext` é uma `BeanFactory`: você pede `getBean(Tipo.class)` e recebe pronto.
- `FactoryBean<T>`: interface para beans que fabricam outros beans.
- Em Spring, uma "factory de estratégias" quase sempre vira um registry de beans, como aqui.

---

## 3. Chain of Responsibility

### O que é

Passar um pedido por **uma fila de tratadores**. Cada um decide: deixa passar, barra, ou resolve sozinho e encerra a fila. Quem envia o pedido não sabe quantos tratadores existem nem a ordem.

Analogia: o controle de segurança do aeroporto. Documento, raio-x, detector de metal. Qualquer etapa pode barrar você. Para acrescentar uma etapa, ninguém reescreve as outras.

### A dor no projeto

Versão feia (commit `2c1aa93`): tamanho, termo proibido e cache dentro do `processar()`, que ia de 10 para 50 linhas, misturando validação, cache e chamada ao provedor. Cada regra nova abriria o mesmo método.

### Como foi aplicado

Pasta [`core/chain/`](../src/main/java/br/com/rafael/aigateway/core/chain/):

| Arquivo | Papel |
|---|---|
| [`PromptHandler`](../src/main/java/br/com/rafael/aigateway/core/chain/PromptHandler.java) | A interface do elo: `void handle(ContextoRequisicao ctx)` |
| [`ContextoRequisicao`](../src/main/java/br/com/rafael/aigateway/core/chain/ContextoRequisicao.java) | O envelope que passa de mão em mão: a requisição, os tokens estimados e uma eventual resposta pronta |
| [`TamanhoHandler`](../src/main/java/br/com/rafael/aigateway/core/chain/TamanhoHandler.java) `@Order(1)` | Estima tokens; acima do limite, barra |
| [`ConteudoHandler`](../src/main/java/br/com/rafael/aigateway/core/chain/ConteudoHandler.java) `@Order(2)` | Barra termo proibido, ignorando acento e maiúscula |
| [`CacheHandler`](../src/main/java/br/com/rafael/aigateway/core/chain/CacheHandler.java) `@Order(3)` | Se já respondeu este prompt neste perfil, grava a resposta no contexto e encerra |
| [`CorrenteDeValidacao`](../src/main/java/br/com/rafael/aigateway/core/chain/CorrenteDeValidacao.java) | Percorre os handlers e para quando alguém respondeu |

As três saídas de um elo:

1. **Aprovar:** retorna sem fazer nada. A corrente segue.
2. **Barrar:** lança `PromptBloqueadoException`. A corrente para e a requisição volta 400, sem tocar no provedor.
3. **Responder:** chama `ctx.responderComCache(...)`. A corrente para e o gateway devolve essa resposta.

**Por que essa ordem?** O mais barato e mais restritivo primeiro. Contar caracteres é quase de graça. O cache fica por último porque não faz sentido devolver do cache um prompt que as regras anteriores barrariam (por exemplo, se a lista de termos proibidos mudar).

### O que o Spring faz por baixo

Os três handlers têm `@Component`, então entram sozinhos na `List<PromptHandler>` da `CorrenteDeValidacao`. Quando a lista é injetada, o Spring **ordena pelo `@Order`**. A ordem é impressa no log na subida (`[chain] corrente montada: [...]`), para conferir.

### Teoria: versão clássica x lista ordenada

**Clássica (livro do GoF):** cada handler guarda o próximo e chama ele.

```java
abstract class Handler {
    private Handler proximo;
    void setProximo(Handler p) { this.proximo = p; }
    void handle(Pedido p) {
        if (proximo != null) proximo.handle(p);
    }
}
class TamanhoHandler extends Handler {
    void handle(Pedido p) {
        if (grande(p)) throw ...;
        super.handle(p);          // passa adiante
    }
}
// montagem na mão:
tamanho.setProximo(conteudo);
conteudo.setProximo(cache);
```

**Lista ordenada (a usada aqui):** um orquestrador percorre a lista.

| | Clássica | Lista ordenada |
|---|---|---|
| Quem conhece a ordem | Cada handler (o "próximo") | O orquestrador (`@Order`) |
| Handler esquece de chamar o próximo | A corrente quebra em silêncio | Impossível, quem chama é o laço |
| Handler pode fazer algo **depois** do próximo | Sim (código após `proximo.handle`) | Não diretamente |
| Montagem | Na mão, encadeando | Automática pelo Spring |

A clássica brilha quando o handler precisa agir na volta (medir tempo, fechar recurso). É o caso dos filtros de servlet.

### Armadilhas

- Handler com estado em campo: os handlers são singletons. O que é da requisição vai no `ContextoRequisicao`, criado por requisição.
- Esquecer o `@Order`: sem ele a ordem é indefinida.
- Usar exceção para tudo: aqui a exceção é só para "barrar". "Responder" usa o contexto, porque cache hit não é erro.

### Onde o Spring usa

- **Spring Security:** a `SecurityFilterChain` é uma corrente de filtros (autenticação, CSRF, autorização), cada um podendo barrar a requisição.
- **Servlet `Filter` e `FilterChain`:** versão clássica, `chain.doFilter(req, res)` passa adiante.
- `HandlerInterceptor` do Spring MVC: `preHandle` devolve `false` para interromper.

---

## 4. Decorator

### O que é

Acrescentar comportamento a um objeto **embrulhando-o** em outro objeto que tem a **mesma interface**. Quem chama não percebe a diferença. As camadas podem ser empilhadas.

Analogia: capa de celular. A capa tem o formato do celular (você usa os mesmos botões) e acrescenta proteção por fora. Dá para pôr película, capa e suporte, um em cima do outro.

### A dor no projeto

O `MockEconomicoProvider` falha em 40% das chamadas. Precisamos de **retry** e de **log com tempo** em todos os provedores. As opções ruins:

- copiar o laço de retry em cada provedor (três cópias, e cada provedor novo precisa lembrar);
- pôr o retry no service (engorda a fachada, e o service passa a saber de falha de rede);
- herança (`RapidoComRetry extends MockRapidoProvider`): uma subclasse para cada combinação de provedor e camada. Três provedores x duas camadas já seriam seis classes.

### Como foi aplicado

Pasta [`provider/decorator/`](../src/main/java/br/com/rafael/aigateway/provider/decorator/):

| Arquivo | Papel |
|---|---|
| [`ProviderDecorator`](../src/main/java/br/com/rafael/aigateway/provider/decorator/ProviderDecorator.java) | Classe abstrata: implementa `AiProvider`, guarda o `envolvido` e repassa `nome()` e `perfil()` |
| [`RetryProviderDecorator`](../src/main/java/br/com/rafael/aigateway/provider/decorator/RetryProviderDecorator.java) | Repete `envolvido.gerar()` até 3 vezes, **só** para `ProvedorIndisponivelException` |
| [`LoggingProviderDecorator`](../src/main/java/br/com/rafael/aigateway/provider/decorator/LoggingProviderDecorator.java) | Mede o tempo e loga sucesso ou falha |

O empilhamento é feito pelo `ProviderRegistry`, na subida:

```java
AiProvider decorado = new LoggingProviderDecorator(
        new RetryProviderDecorator(provedor, maxTentativas));
```

```
chamada → Logging → Retry → MockEconomico
                     ↺ (até 3x)
```

**Por que log por fora e retry por dentro?** Assim o tempo medido inclui todas as tentativas, que é o tempo que o cliente esperou. Invertendo, o log mediria cada tentativa separada.

**Por que os decorators não têm `@Component`?** Porque implementam `AiProvider`. Se fossem beans, o Spring os colocaria na `List<AiProvider>` como se fossem mais três provedores. Quem cria as camadas é o registry.

**Por que o retry só repete `ProvedorIndisponivelException`?** Porque ela significa "falha temporária". Repetir um bug (`IllegalArgumentException`) só gasta tempo e devolve o mesmo erro. Há teste para isso.

**Desvio do plano original:** o plano punha os decorators em `core/decorator/`. Eles foram para `provider/decorator/` porque são provedores (implementam `AiProvider`) e porque quem os monta é o `ProviderRegistry`. Assim o pacote `provider` não depende do `core`.

### Teoria: Decorator x Proxy x herança

Pergunta clássica de entrevista.

| | Decorator | Proxy | Herança |
|---|---|---|---|
| Intenção | **Acrescentar** comportamento | **Controlar o acesso** ao objeto (carregar sob demanda, checar permissão, chamar remoto) | Especializar uma classe |
| Empilha? | Sim, várias camadas | Normalmente uma | Não, combinações explodem em subclasses |
| Quando decide | Em tempo de execução, na montagem | Em tempo de execução | Em tempo de compilação |
| Estrutura | Igual: mesma interface, guarda o real | Igual | `extends` |

A estrutura do Decorator e do Proxy é praticamente a mesma. O que muda é a intenção.

### Onde o Spring usa

- `@Transactional`, `@Cacheable`, `@Async`, `@Retryable`: o Spring cria um **proxy** em volta do bean que abre transação, consulta cache ou repete a chamada antes e depois do método real. É a mesma estrutura, gerada automaticamente.
- Armadilha famosa: chamar um método `@Transactional` **de dentro da mesma classe** não passa pelo proxy, então a transação não abre.
- `BufferedInputStream(new FileInputStream(...))` do Java é o Decorator de livro.

---

## 5. Observer

### O que é

Um objeto **publica um acontecimento** e vários outros reagem, sem que quem publica conheça quem reage.

Analogia: o sino da cozinha. O cozinheiro toca quando o prato sai e volta a cozinhar. O garçom leva o prato, o caixa anota e o gerente confere. Dá para contratar mais gente que ouve o sino sem mudar o cozinheiro.

### A dor no projeto

Registrar uso, alertar custo, amanhã talvez mandar métrica. Se o service chamasse `repository.salvar(...)`, `alerta.verificar(...)` e `metricas.enviar(...)` diretamente, cada reação nova abriria o service, e ele dependeria de classes que não têm nada a ver com gerar resposta.

### Como foi aplicado

Pasta [`usage/`](../src/main/java/br/com/rafael/aigateway/usage/):

| Peça do padrão | No projeto |
|---|---|
| Evento (o sino) | [`UsoRegistradoEvent`](../src/main/java/br/com/rafael/aigateway/usage/UsoRegistradoEvent.java), um `record` imutável: evento é fato que já aconteceu |
| Quem publica (o cozinheiro) | `AiGatewayService.publicarUso()` chama `ApplicationEventPublisher.publishEvent(...)` |
| Observador 1 | [`UsageListener`](../src/main/java/br/com/rafael/aigateway/usage/UsageListener.java): grava no [`UsageRepository`](../src/main/java/br/com/rafael/aigateway/usage/UsageRepository.java) |
| Observador 2 | [`AlertaDeCustoListener`](../src/main/java/br/com/rafael/aigateway/usage/AlertaDeCustoListener.java): avisa no log quando o custo passa do limite |
| Quem lê o resultado | [`UsageController`](../src/main/java/br/com/rafael/aigateway/api/UsageController.java) → `GET /usage` → [`ResumoUso`](../src/main/java/br/com/rafael/aigateway/usage/ResumoUso.java) |

O `AlertaDeCustoListener` existe para mostrar a vantagem: uma reação nova, **zero linhas mudadas** no service e no outro listener.

Cache hit também publica evento (com custo zero), para o `/usage` contar quantas chamadas o cache economizou. Prompt barrado não publica: não houve uso.

### O que o Spring faz por baixo

`ApplicationEventPublisher` é o próprio `ApplicationContext`. Na subida, o Spring procura métodos com `@EventListener` e anota o tipo do parâmetro. Quando alguém publica um objeto, ele chama todos os listeners daquele tipo.

### Teoria

- **Síncrono por padrão.** O listener roda **na mesma thread** e **antes** de `publishEvent()` retornar. Se ele demorar, a resposta ao cliente demora junto. Se ele lançar exceção, ela sobe para o service.
- **Assíncrono:** `@Async` no listener + `@EnableAsync` na aplicação faz rodar em outra thread. O custo: a exceção não volta para quem publicou, e a ordem deixa de ser garantida.
- **Cuidado com transação:** se o listener grava em banco e o publicador está numa transação que depois sofre rollback, o listener já gravou algo que "não aconteceu". Solução do Spring: `@TransactionalEventListener(phase = AFTER_COMMIT)`, que só roda depois do commit.
- **Observer x Pub/Sub:** no Observer, os dois lados estão no mesmo processo. No Pub/Sub (Kafka, RabbitMQ), há um intermediário e eles podem estar em máquinas diferentes.

### Onde o Spring usa

- `ApplicationReadyEvent`, `ContextRefreshedEvent`: o Spring publica eventos do ciclo de vida da aplicação.
- Spring Security publica `AuthenticationSuccessEvent` e `AuthenticationFailureEvent`.

---

## 6. Facade

### O que é

Uma **porta de entrada simples** para um conjunto de subsistemas complicados. Quem usa chama um método e não precisa saber quantas peças existem por trás.

Analogia: o balcão do restaurante. Você faz o pedido num lugar só e não fala com a cozinha, o estoque e o caixa separadamente.

### Como foi aplicado

[`AiGatewayService`](../src/main/java/br/com/rafael/aigateway/core/AiGatewayService.java). O controller chama só `processar(requisicao)`. Por trás, a fachada coordena quatro subsistemas, e cada um é um padrão:

```java
ContextoRequisicao ctx = new ContextoRequisicao(requisicao);
corrente.executar(ctx);                              // 1. Chain
if (ctx.respostaPronta().isPresent()) { ... }        //    cache hit
AiProvider provedor = registry.obter(perfil);        // 2. Factory + Strategy (+ Decorator por dentro)
RespostaIa gerada = provedor.gerar(prompt);
cache.guardar(requisicao, resposta);                 // 3. cache
publicarUso(requisicao, resposta);                   // 4. Observer
```

O critério de sucesso: `processar()` **não tem regra de negócio dentro**. Nenhum `if` de perfil, de tamanho, de termo ou de retry. Só a ordem dos passos. Cada regra mora no subsistema dela.

O controller, por sua vez, é **magro**: recebe, valida o formato (`@Valid`) e chama a fachada. Se amanhã o gateway for chamado por fila em vez de HTTP, só o controller muda.

### Comparação com o laboratório

- **Lab Java:** `Facade.migrarCliente(nome, cep)` esconde `CepApi` e `CrmService`.
- **Lab Spring:** `ClienteServiceImpl` esconde dois repositórios e o `ViaCepService`.
- **Aqui:** mesma ideia. A diferença é que os subsistemas escondidos são, eles mesmos, outros padrões.

### Armadilhas

- **Fachada "deus":** se todas as regras forem morar na fachada, ela vira o service gigante que os outros padrões tiraram. A fachada coordena, não decide.
- Facade não proíbe o acesso direto aos subsistemas. Os testes, por exemplo, usam a `CorrenteDeValidacao` direto.

---

## 7. Singleton

### O que é

Garantir que uma classe tenha **um único objeto** na aplicação inteira, com um ponto de acesso a ele.

### Como foi aplicado

**Nenhum singleton foi escrito na mão.** Todo `@Component`, `@Service`, `@Repository` e `@RestController` é, por padrão, um bean de **escopo singleton**: o container do Spring cria um objeto de cada na subida e entrega o mesmo para todo mundo que pedir.

O teste `AiGatewayIntegrationTest.beansSaoSingletons()` prova isso: pede o `AiGatewayService` duas vezes ao contexto e confere que é o **mesmo objeto** (`isSameAs`).

### Comparação com o laboratório

O lab Java mostra três formas na mão:

| Forma | Como | Problema |
|---|---|---|
| `SingletonEager` | `static final` criado na carga da classe | Cria mesmo se ninguém usar |
| `SingletonLazy` | Cria no primeiro `getInstancia()` | Sem `synchronized`, duas threads podem criar dois |
| `SingletonLazyHolder` | Classe interna estática guarda a instância | Seguro e preguiçoso; é a melhor das três em Java puro |

**Por que não fazer na mão em Spring:**

1. O container já garante um objeto só.
2. Singleton na mão (`getInstancia()` estático) é **difícil de testar**: não dá para trocar por um falso. Os beans daqui são classes comuns, e os testes dão `new` neles com dependências falsas.
3. O singleton do Spring é "um por container", não "um por JVM". Isso é suficiente e mais flexível.

### O perigo real: estado mutável em campo

Como o bean é compartilhado por **todas as requisições ao mesmo tempo** (cada requisição numa thread), qualquer campo que mude por requisição é bug de concorrência:

```java
@Service
class ServiceComBug {
    private int tokensDaRequisicaoAtual;   // ERRADO: duas requisições sobrescrevem uma à outra
}
```

Regras seguidas no projeto:

- Campos de beans são `final` e apontam para outros beans ou configuração.
- O que é da requisição vive em **variável local** ou num objeto criado por requisição (`ContextoRequisicao`).
- Onde o estado compartilhado é intencional, a estrutura é segura para threads: `ConcurrentHashMap` no [`CacheDeRespostas`](../src/main/java/br/com/rafael/aigateway/core/CacheDeRespostas.java) e `CopyOnWriteArrayList` no `UsageRepository`.

### Outros escopos de bean

`prototype` (um objeto novo a cada pedido ao container), `request` (um por requisição HTTP), `session` (um por sessão). Raramente necessários. Singleton sem estado mutável resolve quase tudo.

---

## 8. Uma requisição, arquivo por arquivo

`POST /completions` com `{"prompt":"explique records","perfil":"ECONOMICO"}`:

1. O Tomcat recebe. O `DispatcherServlet` do Spring acha o método com `@PostMapping("/completions")` em [`CompletionController`](../src/main/java/br/com/rafael/aigateway/api/CompletionController.java).
2. O **Jackson** converte o JSON em [`CompletionRequest`](../src/main/java/br/com/rafael/aigateway/api/dto/CompletionRequest.java). `"ECONOMICO"` vira `Perfil.ECONOMICO`. Texto fora do enum: 400 aqui.
3. `@Valid` roda `@NotBlank` e `@NotNull`. Falhou: 400 com a lista de campos ([`GlobalExceptionHandler`](../src/main/java/br/com/rafael/aigateway/api/GlobalExceptionHandler.java)).
4. O controller chama `AiGatewayService.processar()` (**Facade**).
5. A fachada cria o `ContextoRequisicao` e chama `CorrenteDeValidacao.executar()` (**Chain**): `TamanhoHandler` → `ConteudoHandler` → `CacheHandler`.
6. Sem cache hit, a fachada chama `ProviderRegistry.obter(ECONOMICO)` (**Factory**), que devolve o provedor já decorado.
7. `provedor.gerar()` entra no `LoggingProviderDecorator`, que chama o `RetryProviderDecorator` (**Decorator**), que chama o `MockEconomicoProvider` (**Strategy**). Se ele falhar, o retry tenta de novo.
8. A fachada monta o `CompletionResponse`, guarda no `CacheDeRespostas` e publica `UsoRegistradoEvent` (**Observer**).
9. `UsageListener` grava e `AlertaDeCustoListener` confere o custo, na mesma thread.
10. O controller devolve o record. O Jackson transforma em JSON. 200.

Para ver isso acontecendo, rode a aplicação e acompanhe o console: cada etapa loga com o prefixo do padrão (`[chain]`, `[strategy]`, `[decorator]`, `[observer]`).

---

## 9. Os testes de cada padrão

| Padrão | Teste | O que prova |
|---|---|---|
| Strategy + Factory | `ProviderRegistryTest.entregaUmProvedorDiferenteParaCadaPerfil` | Cada perfil cai no provedor certo |
| Factory | `ProviderRegistryTest.doisProvedoresNoMesmoPerfilImpedemASubida` | Configuração errada quebra na subida |
| Chain | `CorrenteDeValidacaoTest` (5 testes) | Barra por tamanho e por termo (com acento), cache encerra a corrente, perfil diferente não é hit |
| Chain + Facade | `AiGatewayServiceTest.promptBarradoNaoChegaNoProvedorNemGeraEvento` | Prompt barrado nunca chega no registry |
| Decorator | `RetryProviderDecoratorTest` (4 testes) | Tenta até responder, desiste no limite, não repete bug, repassa nome e perfil |
| Decorator + Factory | `ProviderRegistryTest.oProvedorEntregueJaVemComRetry` | O registry entrega o provedor já embrulhado |
| Observer | `AiGatewayServiceTest.caminhoFelizChamaOProvedorEPublicaUmEvento` | Um evento por resposta, com os dados certos |
| Observer | `UsageObserverTest` | Listener grava e o resumo agrega |
| Singleton | `AiGatewayIntegrationTest.beansSaoSingletons` | O mesmo objeto nas duas buscas |
| Tudo junto | `AiGatewayIntegrationTest` | HTTP de ponta a ponta com MockMvc |

**Testar comportamento, não implementação.** Os testes perguntam "o provedor foi chamado?", "qual foi a resposta?", e não "qual método privado rodou". Assim um refactor interno não quebra teste.

Os testes de unidade não sobem o Spring: dão `new` nas classes e passam um `ProvedorFalso` (provedor de mentira, instantâneo, que pode falhar N vezes). Isso só é possível porque nada aqui é singleton feito na mão.

---

## 10. Perguntas de entrevista

**"Qual a diferença entre Strategy e Factory no seu projeto?"**
Strategy separa os jeitos de gerar resposta, um por classe, todos atrás de `AiProvider`. Factory decide qual desses jeitos usar para cada perfil. Um não substitui o outro: sem o Strategy, a factory não teria o que entregar; sem a factory, o `if` de escolha voltaria para o service.

**"Por que Chain of Responsibility e não três `if` no service?"**
Porque cada regra nova abriria o mesmo método, e ele misturava validação, cache e chamada ao provedor. Com a corrente, regra nova é classe nova com `@Order`. E dá para testar cada regra sozinha.

**"Decorator e Proxy não são a mesma coisa?"**
A estrutura é igual: mesma interface, guarda o objeto real. A intenção muda: Decorator acrescenta comportamento e empilha; Proxy controla o acesso. O `@Transactional` do Spring é um proxy que se comporta como decorator.

**"Seu listener é síncrono. Qual o risco?"**
Se ele demorar, a resposta demora; se lançar exceção, a requisição falha mesmo com a IA tendo respondido. Para algo lento, `@Async`. Se gravasse em banco dentro de transação, `@TransactionalEventListener(AFTER_COMMIT)`.

**"Onde está o Singleton? Não vejo `getInstancia()`."**
Em todo bean. O Spring cria um de cada. Não escrevo na mão porque o container já garante e porque singleton estático não dá para substituir em teste. O cuidado real é não pôr estado de requisição em campo.

**"O que você faria diferente em produção?"**
Cache com expiração e limite (Caffeine ou Redis), histórico de uso em banco, retry com espera crescente entre tentativas (*backoff*) e *circuit breaker* (Resilience4j), tokenizador real do modelo, e o listener de uso assíncrono.
