# Contexto do projeto: ai-gateway

Você vai me acompanhar no desenvolvimento de um projeto de estudo. Leia tudo antes de fazer qualquer coisa.

## Quem sou eu

Sou o Rafael, desenvolvedor júnior, estagiário ABAP, estudando Java/Spring Boot para chegar a backend pleno e depois AI Engineer. Tenho base de Spring (já construí uma API de clínica com JPA, validação, exceptions e testes), mas nunca implementei design patterns na mão de forma consciente. Falo português informal, prefiro respostas diretas, honestas e estruturadas, com analogias simples antes do código. Não use travessão (em dash) nos textos.

## O que é o projeto

Entrega do desafio de Padrões de Projeto do bootcamp "Java com IA" da DIO em parceria com o Itaú. O objetivo principal NÃO é só entregar: é eu aprender design patterns de verdade construindo.

O `ai-gateway` é uma API Spring Boot que fica no meio entre uma aplicação cliente e provedores de IA. A aplicação fala só com o gateway, e ele cuida do resto. Problemas que ele resolve:

1. Trocar de provedor ou usar um mais barato para tarefas simples, sem mexer no código cliente
2. Barrar prompts ruins antes de chegar na IA: muito grandes (custo), com termos proibidos, ou repetidos (cache)
3. Tentar de novo quando o provedor falha
4. Registrar quanto foi gasto, com qual provedor, em qual requisição

IMPORTANTE: os provedores são MOCKS. Sem chave de API, sem chamada HTTP externa, sem custo. Latência e falhas são simuladas.

## O desafio da DIO (requisitos reais)

Enunciado: consolidar Padrões de Projeto com algo que agregue valor ao portfólio. Três abordagens aceitas: reproduzir e evoluir o projeto da aula, criar do zero (Java puro ou Spring), ou focar em um único padrão. Eu escolhi CRIAR DO ZERO com Spring. Fork dos laboratórios é sugestão, não obrigação.

Laboratórios oficiais da aula (use como referência de estilo, não como base do código):
- https://github.com/digitalinnovationone/lab-padroes-projeto-java (Singleton, Strategy, Facade em Java puro)
- https://github.com/digitalinnovationone/lab-padroes-projeto-spring (Singleton, Strategy/Repository, Facade com Spring)

Minhas notas da aula (Strategy, Facade) ficam no cofre Obsidian:
`~/Obsidian/rocha-karpathy/🧮-50-estudos/cursos/dio-itau-ia/Módulo - Design Patterns/`

Implicações:
- Os três padrões da aula (Singleton, Strategy, Facade) precisam estar claramente identificáveis no código e no README. Quando chegarmos neles, compare minha implementação com a do laboratório e me mostre as diferenças.
- Factory, Chain of Responsibility, Decorator e Observer são a minha evolução em relação à aula. O README deve deixar essa divisão explícita.
- Como o enunciado aceita escopo reduzido, parar no Módulo 3 já é entrega válida.

## Stack

- Java 21 + **Spring Boot 4.1.1** + Maven (wrapper `./mvnw`)
- Dependências: `spring-boot-starter-webmvc`, `spring-boot-starter-validation` e os starters de teste correspondentes (`-webmvc-test`, `-validation-test`)
- Sem banco, sem Docker. Repositório de uso em memória
- Swagger via springdoc no final (Boot 4 exige a linha 3.x do springdoc)

**Nota de 2026-09-28:** o plano original dizia Spring Boot 3, mas o start.spring.io já não oferece a linha 3 (3.5 saiu do suporte OSS em meados de 2026). Por isso a versão é a 4.1.1. No Boot 4, o starter antigo `spring-boot-starter-web` virou `spring-boot-starter-webmvc`. Para este projeto, os padrões e as anotações usadas não mudam.

## Fluxo de uma requisição

POST /completions → Pipeline de regras (Chain of Responsibility) → Escolha do provedor (Strategy + Factory) → Camadas extras de retry e log (Decorator) → Provedor mock → Resposta + evento de uso publicado (Observer). Tudo orquestrado por uma fachada (Facade). Beans do Spring são singletons (Singleton).

Exemplo de requisição:
```json
{ "prompt": "Explique o que e injecao de dependencia", "perfil": "RAPIDO" }
```

Exemplo de resposta:
```json
{
  "id": "c4f1a2b8",
  "resposta": "Injecao de dependencia e quando...",
  "provedorUsado": "MOCK_RAPIDO",
  "tokensGastos": 148,
  "custoEstimado": 0.0012,
  "cacheHit": false,
  "duracaoMs": 312
}
```

Log esperado no console, mostrando os padrões trabalhando:
```
[chain] TamanhoHandler: 42 tokens, aprovado
[chain] ConteudoHandler: nenhum termo bloqueado
[chain] CacheHandler: miss, seguindo
[strategy] perfil RAPIDO resolvido para MOCK_RAPIDO
[decorator] tentativa 1 de 3
[decorator] concluido em 312ms
[observer] uso registrado: 148 tokens
```

Três caminhos possíveis: caminho feliz; requisição barrada pela Chain sem chegar na IA; cache hit respondendo em poucos ms com custo zero.

## Estrutura de pacotes (nome de domínio, não de camada técnica)

```
br.com.rafael.aigateway/
├── api/           CompletionController, UsageController, dto/ (CompletionRequest, CompletionResponse)
├── core/          AiGatewayService (Facade)
│   ├── chain/     PromptHandler, TamanhoHandler, ConteudoHandler, CacheHandler
│   └── decorator/ RetryProviderDecorator, LoggingProviderDecorator
├── provider/      AiProvider (Strategy), ProviderRegistry (Factory), MockRapidoProvider, MockPremiumProvider
└── usage/         UsoRegistradoEvent, UsageListener, UsageRepository
```

## Como você deve me ensinar (REGRA MAIS IMPORTANTE)

NÃO escreva o código do projeto por mim. Se você me entregar código pronto, eu tenho um projeto e zero aprendizado. Siga este ciclo em cada módulo:

1. **Você apresenta a dor.** Eu escrevo a versão feia de propósito (if/else, tudo no service). Sentir o problema é o que faz o padrão grudar.
2. **Eu tento o refactor.** Você dá a direção (quais classes, quais responsabilidades), não o código.
3. **Eu mostro o que fiz.** Você lê meus arquivos, revisa, aponta o que está fora do padrão e explica o porquê.
4. **Você fecha a teoria.** Nome formal, variações, armadilhas comuns, e onde o próprio Spring usa esse padrão internamente.

Exceções: você pode escrever código de setup trivial (pom.xml, application.properties) e pode me dar o código se eu pedir explicitamente DEPOIS de ter tentado. Se eu pedir antes de tentar, me lembre da regra.

Quando eu disser "estou no módulo X, fiz Y, travei em Z", retome daí. Explique o que está acontecendo enquanto eu programo: o que o Spring está fazendo por baixo, por que algo compila ou quebra. Seja honesto quando eu estiver forçando um padrão onde não precisa.

## Roadmap (21h no total, um commit por módulo)

### Módulo 0: Fundação (1h30, sem padrão)
- Projeto no start.spring.io (Web, Validation, Test)
- `CompletionRequest` e `CompletionResponse` como `record`
- `CompletionController` com POST /completions devolvendo texto fixo
- `@Valid` com `@NotBlank` no prompt
- Checkpoint: `curl -X POST localhost:8080/completions -H "Content-Type: application/json" -d '{"prompt":"teste"}'` devolve 200
- Commit: `feat: setup do projeto e endpoint de completions`

### Módulo 1: Strategy (3h, padrão da aula)
- Estudo prévio: programar para interface, não para implementação. Ler o Strategy do laboratório Java puro da DIO
- Dor: `if (perfil.equals("RAPIDO")) {...} else if (...)` com a lógica dos dois provedores dentro do service
- Refactor: interface `AiProvider` com `gerar(String prompt)` e `nome()`; record `RespostaIa(String texto, int tokens, double custo)`; `MockRapidoProvider` e `MockPremiumProvider` com latência simulada; injetar `List<AiProvider>`
- Teoria: como o Spring injeta lista de implementações; princípio aberto/fechado; comparação com o Strategy do laboratório
- Commit: `refactor: extrai provedores para Strategy`

### Módulo 2: Factory (1h30, evolução)
- Dor: alguém precisa decidir qual provider usar; se ficar no service, volta o if
- Construir: enum `Perfil { RAPIDO, PREMIUM }`; `ProviderRegistry` montando `Map<Perfil, AiProvider>` no construtor; exception própria para perfil inexistente
- Teoria: Simple Factory vs Factory Method vs Abstract Factory; por que no Spring vira registry de beans
- Commit: `feat: registry de provedores`

### Módulo 3: Chain of Responsibility (4h, evolução, o mais pesado e mais valioso)
- Estudo prévio: implementar o padrão em Java puro num pacote de rascunho separado (ex: `estudos/chain`), fora do projeto, já que o laboratório da DIO não cobre esse padrão. Referência externa: github.com/iluwatar/java-design-patterns
- Dor: as três validações (tamanho, termo proibido, cache) dentro do service, método indo de 10 para 50 linhas
- Refactor: interface `PromptHandler` com `handle(ContextoRequisicao ctx)`; `TamanhoHandler`, `ConteudoHandler`, `CacheHandler` com `@Order`; `List<PromptHandler>` ordenada; mecanismo para um handler interromper a corrente e devolver resposta
- Teoria: versão clássica (ponteiro para o próximo) vs lista ordenada; Spring Security como corrente de filtros
- Dois commits: versão feia e refactor (o histórico vira narrativa do README)

### Módulo 4: Decorator (3h, evolução)
- Dor: retry e log em todos os providers sem duplicar e sem engordar o service
- Construir: `MockInstavelProvider` falhando em 40% das chamadas; `RetryProviderDecorator` implementando `AiProvider` e envolvendo outro; `LoggingProviderDecorator` medindo tempo; empilhar as camadas
- Teoria: Decorator vs Proxy vs herança (clássico de entrevista); `@Transactional` e `@Cacheable` por baixo
- Commit: `feat: retry e log via Decorator`

### Módulo 5: Observer (2h, evolução)
- Dor: o service chamando repositório, log e métrica diretamente
- Construir: record `UsoRegistradoEvent`; `ApplicationEventPublisher` no service; `UsageListener` com `@EventListener`; `UsageRepository` em memória; GET /usage
- Teoria: eventos síncronos vs `@Async`; cuidado com transação quando o listener grava em banco
- Commit: `feat: registro de uso via eventos`

### Módulo 6: Facade e Singleton (1h, padrões da aula)
- `AiGatewayService` como fachada única: corrente, escolha do provider, publicação do evento
- Controller magro, só recebe e devolve
- Teoria: por que não implementar Singleton na mão; escopo de bean; o perigo de estado mutável em campo de service; comparação com Facade e Singleton dos dois laboratórios da DIO
- Commit: `refactor: fachada do gateway`

### Módulo 7: Testes (3h)
- Estudo prévio: JUnit 5 e Mockito básico
- Um teste por padrão: barra prompt longo sem chamar provedor (Chain); troca de provedor conforme perfil (Strategy + Factory); tenta três vezes quando falha (Decorator); publica evento após cada chamada (Observer)
- Teoria: testar comportamento, não implementação
- Commit: `test: cobertura dos padroes aplicados`

### Módulo 8: Entrega (2h)
- README com tabela padrão por padrão: problema, solução, link para o arquivo, e se é padrão da aula ou evolução
- Seção "Relação com o laboratório da DIO": tabela com Singleton, Strategy e Facade mostrando como foi feito no lab e como foi feito no ai-gateway
- Bloco "antes e depois" do Módulo 3
- Diagrama Mermaid no README
- Swagger com springdoc
- Revisar histórico de commits, publicar e submeter na DIO

## Ordem de estudo sugerida
Programar para interface → Strategy → Chain of Responsibility → Decorator → Observer. Factory, Facade e Singleton se aprendem no caminho.

## Estado atual

- **2026-09-28:** projeto gerado (Maven, Boot 4.1.1, Java 21, pacote `br.com.rafael.aigateway`, Web + Validation). `./mvnw package` passa.
- **2026-09-29:** Módulo 0 concluído. Commit `9ce2dab` publicado em https://github.com/rafaelrch/ai-gateway (branch `main`). Decisões tomadas: `custoEstimado` é `BigDecimal` (criado sempre a partir de String), `duracaoMs` é `long`, `tokensGastos` é `int`. Checkpoint validado: 200 com prompt, 400 com prompt vazio ou ausente.
- **2026-09-30:** Módulo 1, etapa 1 (estudo prévio) concluída. A nota `Strategy.md` do cofre foi reescrita para leigo (furadeira e brocas, exemplo de frete, lab da DIO) e ele respondeu as perguntas: conceito firme; a pergunta 5 (bean único com setter, condição de corrida) ficou aberta para o refactor.
- **2026-10-01:** Módulo 1, etapa 2 (versão feia) concluída. `AiGatewayService` com `if/else` de perfil, `Thread.sleep`, `throws InterruptedException` subindo até o controller; controller com injeção por construtor. Checkpoint na 8081: RAPIDO 200 (105ms), PREMIUM 200 (800ms), perfil inválido 500, sem `perfil` 500 por `NullPointerException`. O service ainda está em `api/core` (mover no refactor).
- **2026-10-02:** Módulo 1, etapas 3 e 4 (refactor e revisão) concluídas. `provider/` com `AiProvider` (`gerar`, `nome`), `RespostaIa(texto, tokens, custo)`, `MockRapidoProvider` e `MockPremiumProvider` com `@Component` e `InterruptedException` tratada dentro (catch, `interrupt()`, `IllegalStateException`). Service em `core/` recebe `List<AiProvider>` e escolhe por `nome().equals("MOCK_" + perfil)`, sem `if` de perfil. Checkpoint na 8081: RAPIDO 200, PREMIUM 200, XYZ e sem perfil 500 com `Perfil desconhecido`. `.idea` recriada só com Maven (a antiga misturava Gradle). `.DS_Store` no `.gitignore`.
- **2026-10-02:** **Módulo 1 concluído.** Versão feia em `5cf3d80` (commitada por ele, prefixo `add:` fora do padrão, já publicada), refactor em `4bec2f5`. Teoria escrita na seção 12 da nota `Strategy.md` do cofre. Pendentes do M1: prova do `MockEconomicoProvider` (adiada por decisão dele, sem commit) e perguntas 6 e 7 da nota.
- **2026-10-03:** Módulo 2 (Factory), etapas 1 a 3 concluídas. `Perfil { RAPIDO, PREMIUM }` em `provider/`; `CompletionRequest.perfil` virou `@NotNull Perfil`; `AiProvider` ganhou `Perfil perfil()`; `ProviderRegistry` monta `HashMap<Perfil, AiProvider>` no construtor, falha na subida se dois provedores declaram o mesmo perfil (`put` devolve o anterior, `IllegalStateException`) e expõe `obter(Perfil)`; `PerfilNaoSuportadoException` (unchecked); `GlobalExceptionHandler` em `api/` devolvendo `ProblemDetail` 400. Service recebe o registry, sem `for`. Checkpoint na 8080: RAPIDO 200, PREMIUM 200, XYZ 400 (Jackson), sem perfil 400 (`@NotNull`), `ECONOMICO` no enum sem mock 400 pelo handler (testado e removido). O registry e o handler foram escritos por mim a pedido dele, depois de duas tentativas (mapa como variável local; `put` com `nome()` em vez do provedor).
- **Próximo:** commit `feat: registry de provedores` (com este `CLAUDE.md`), depois etapa 4 do M2 (teoria: Simple Factory, Factory Method, Abstract Factory, registry de beans).
- **Ponto de atenção do Módulo 2:** a ideia de registry só pegou com a analogia do quadro de chaves da portaria. Confunde declarar campo com criar o objeto (`new`), e variável local com campo. Pediu revisão duas vezes sem compilar antes.
- **Ponto de atenção do Módulo 1:** silencia erro do compilador em vez de ler (removeu o `throw`, `horaFim = 0`); usou `new` no lugar de injeção. Também: a explicação só pegou com analogia física e exemplo fora do domínio do projeto. Não pressupor que ele lê UML ou entende interface de cara.
- **Pontos de atenção observados no Módulo 0:** confundiu sintaxe de `record` com classe (campos no corpo, aceitou o "make static" do IntelliJ); ler a sugestão da IDE antes de aceitar. Rodar o compilador antes de pedir revisão.

## Roadmap visual

Página de acompanhamento: https://claude.ai/artifact/H8nh971azdjuFwwV5cWjjp
Arquivo fonte: `~/Obsidian/rocha-karpathy/🧩-40-projetos/ai-gateway/roadmap.html`.

**Atualize ao fim de cada etapa do ciclo**, não só de cada módulo: mude `ESTADO` (`moduloAtual`, `etapaAtual`, `atualizado`) no topo do script, preencha `feito` do módulo concluído, acrescente uma linha em `HISTORICO`, reescreva o bloco "Seu próximo passo" e republique o mesmo arquivo pela ferramenta Artifact com o `url` acima.
