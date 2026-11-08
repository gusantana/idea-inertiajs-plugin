---
name: Svelte 5 & InertiaJS v3 Plugin Development
description: Guia de port do plugin Inertia.js de 2022.3.1 para PhpStorm 2026.2.4, com implementação de suporte a Svelte 5 e Inertia v3
---

# Skill: Port do plugin Inertia.js + Svelte 5/Inertia v3

Use esta skill ao portar o plugin `idea-inertiajs-plugin` para PhpStorm 2026.2.4 e ao implementar suporte a Svelte 5/Inertia v3.

## Regra de ouro

**Port primeiro, features depois.** Não misture a migração da baseline com a implementação de Svelte 5/Inertia v3.

1. Atualize a baseline (Gradle, Kotlin, JDK, plataforma) e faça o build passar.
2. Rode os testes existentes e adicione um smoke test.
3. Só então implemente as fases de Svelte/Inertia v3.

## Estado atual (baseline 2026.2.4)

Baseline já portada e verde (`compileKotlin`, `buildPlugin`, `check`).

O que existe hoje:

- Referências PHP para `Inertia::render('Page')`, `inertia('Page')` e `Route::inertia('/url', 'Page')`.
- Resolução de páginas O(1) por caminho e completion via `FilenameIndex` (`InertiaPageResolver`).
- Line marker para arquivos de página (`js`/`jsx`/`ts`/`tsx`/`vue`/`svelte`); `.svelte` só quando o plugin Svelte está instalado.
- Detecção de projeto via `composer.json`/`package.json`.
- Settings em `.idea/inertia.xml`.

O que **não** existe (ou está incompleto):

- Tratamento das features do Inertia v3 que mudam props (deferred, once, merge, optional, always) em inspeções.
- Cobertura de teste para `Inertia::render()`/`Route::inertia()`: dependem do `PhpIndex` resolvendo as classes, então só `inertia()` é testado ponta a ponta.
- Direção Svelte → PHP da inspeção (descartada de propósito: shared props e partial reloads).

Não descreva essas features como prontas. Se precisar delas, implemente.

## Alvo do port

- PhpStorm 2026.2.4 até 2026.3 (`since-build` 262, `until-build` 263.*), IntelliJ IDEA Ultimate idem.
- Compilação contra a **menor** plataforma suportada (2026.2.4); verificação também contra 2026.3.
- IntelliJ Platform Gradle Plugin 2.x.
- Kotlin 2.x, JDK 21.
- Gradle wrapper compatível com o plugin 2.x.

## Estratégia Svelte: A (plugin Svelte + PSI)

Não escreva um parser/lexer de Svelte. Use o PSI do plugin Svelte.

Plugin escolhido: **`dev.blachut.svelte.lang`** (Svelte, JetBrains).

- Versão compatível com PhpStorm 2026.2.4: `262.10968.63` (`since=262.10968`, `until=262.*`, PHPSTORM `2026.2.3 — 2026.2.4`). Confirmado no `plugin.xml` do artefato.
- Language IDs: `SvelteHTML` (arquivo `.svelte`), `SvelteJS`/`SvelteTS` (blocos `<script>`). File type: `Svelte`, extensão `svelte`.
- Depende de `com.intellij.modules.ultimate`, `JavaScript` e `com.intellij.css` — ok no PhpStorm.

Alternativas (só se o plugin da JetBrains não puder ser usado):

- `io.github.tsessebe.svelte` — Svelte 5 Toolkit, MIT, 2026.2+, language `SvelteHTML`. Incompatível com `dev.blachut.svelte.lang`.
- `com.langheroes.svelte` — Svelte Hero, pago, suporte a Svelte 5.

Se trocar de plugin, ajuste o language ID conforme o `plugin.xml` do plugin escolhido.

Implementação:

- Declare dependência **opcional** no `plugin.xml`:

  ```xml
  <depends optional="true" config-file="svelte-support.xml">dev.blachut.svelte.lang</depends>
  ```

- Coloque as extensões Svelte em `src/main/resources/META-INF/svelte-support.xml`.
- Registre o line marker para a linguagem do arquivo Svelte (`SvelteHTML` no plugin da JetBrains):

  ```xml
  <codeInsight.lineMarkerProvider language="SvelteHTML"
      implementationClass="nz.hailwood.inertiajs.InertiaRenderLineMarkerProvider"/>
  ```

- O provider deve aceitar `PsiFile` (não só `JSFile`) e checar `.svelte`/`JSFile`.
- Se o plugin Svelte não estiver instalado, o plugin principal deve continuar funcionando.

## Fases de implementação

### Fase 0 — Baseline

Valores aplicados no port (verificados em nov/2026):

- `platformType = PS`, `platformVersion = 2026.2.4` (build `262.11879.104`), compilando contra a menor versão suportada.
- `pluginSinceBuild = 262`, `pluginUntilBuild = 263.*` (2026.2 e 2026.3; o 2026.3 é `263.7307.215` no PhpStorm e `263.7307.200` no IDEA).
- `gradleVersion = 9.1.0` (o plugin 2.x exige Gradle 9.0+).
- IntelliJ Platform Gradle Plugin `2.19.0`.
- Kotlin `2.4.21` (a plataforma 2026.2 embarca Kotlin 2.4).
- JDK 21 (`jvmToolchain(21)`).
- `platformBundledPlugins = com.jetbrains.php, JavaScript, com.intellij.css, org.jetbrains.plugins.vue`.

Checklist:

- Atualize `gradle.properties` (`platformType`, `platformVersion`, `pluginSinceBuild`, `pluginUntilBuild`, `platformBundledPlugins`, `platformPlugins`, `gradleVersion`).
- Atualize `build.gradle.kts` para o DSL `intellijPlatform { ... }`: `create(IntelliJPlatformType.PhpStorm, ...)` em `dependencies`, `pluginConfiguration`, `signing`, `publishing`, `pluginVerification`.
- Tarefas renomeadas: `runPluginVerifier` → `verifyPlugin`; `listProductsReleases` não existe mais; `runIdeForUiTests` virou uma task custom via `intellijPlatformTesting.runIde.registering`.
- Rode `./gradlew buildPlugin` e `./gradlew check` antes de qualquer feature.
- Mantenha a descrição do `plugin.xml` vindo dos marcadores `<!-- Plugin description -->` do `README.md` (agora via `pluginConfiguration.description`).

### Fase 1 — Índice de páginas ✅

Implementado em `navigation/InertiaPageResolver.kt`:

- `resolve(project, name)`: monta `pagesRoot/name.<ext>` e desce por `VirtualFile.findChild` — O(1), sem índice.
- `allPages(project)`: `FilenameIndex.getAllFilesByExt` + filtro por `VfsUtil.isAncestor(pagesRoot, ...)`, ordenado por `displayPath`.
- `DumbService.isDumb` evita consultar índice em dumb mode.
- `InertiaPageReference` apenas delega para o resolver.
- `displayPath` continua em `Extensions.kt` (remove a extensão, então `.svelte` funciona).

### Fase 2 — Svelte no line marker ✅

- `plugin.xml` declara `<depends optional="true" config-file="svelte-support.xml">dev.blachut.svelte.lang</depends>`.
- `META-INF/svelte-support.xml` registra `InertiaRenderLineMarkerProvider` para `language="SvelteHTML"`.
- O provider aceita qualquer arquivo cuja extensão esteja em `InertiaPageResolver.pageExtensions`.
- O plugin principal continua funcionando sem o plugin Svelte (dependência opcional).

### Fase 3 — Inertia v3 no PHP ✅ (API de página)

A API de página continua a mesma:

- `Inertia::render('Page', [...])`
- `inertia('Page', [...])`
- `Route::inertia('/url', 'Page', [...])`

Implementado:

- **Named arguments** (PHP 8) via `ParameterList.getParameter("component", position)`:
  - `Inertia::render(component: 'Page')`, `inertia(component: 'Page')`, `Route::inertia(uri: '/url', component: 'Page')`.
  - A posição posicional é 0 para `render`/`inertia()` e 1 para `Route::inertia`.
  - `Route::inertia(uri: '/url')` **não** resolve a URI como página.
- `getParameter(name, index)` devolve o argumento nomeado quando existe, senão o posicional.

Ainda pendente: extrair chaves de props de `Inertia::defer`, `->once()`, `Inertia::merge`, `Inertia::optional`, `Inertia::always` para usar na Fase 4.

### Fase 4 — Props Svelte 5 ✅ (parcial)

Implementado:

- `props/InertiaPropsExtractor.kt`
  - `phpPropKeys(propsArgument)`: chaves do array de props (`ArrayCreationExpression.hashElements`, chave `StringLiteralExpression`).
  - `sveltePropNames(page)`: encontra a chamada `$props()` no PSI de JavaScript do `<script>` (o plugin Svelte parseia o bloco como `SvelteJS`/`SvelteTS`, então os nós são `JSCallExpression`) e lê o padrão de desestruturação imediatamente antes.
  - Aceita `let { users, roles: userRoles, 'page-title': pageTitle } = $props();` → `[users, roles, page-title]`.
- `props/InertiaPropCompletionContributor.kt`
  - Registrado em `plugin.xml` para `language="PHP"`.
  - Ao digitar a **chave** de um item do array de props em `Inertia::render('Page', ['<caret>' => ...])`, sugere as props do `$props()` da página Svelte alvo.
  - Funciona com `Inertia::render`, `inertia()` e `Route::inertia`, inclusive com named arguments (`props:`).
  - Se a página não for `.svelte` (ou o plugin Svelte não estiver instalado), não sugere nada.
- `navigation/InertiaCall.kt` centraliza o reconhecimento das três chamadas e a localização de `$component`/`$props`.

Ainda pendente:

- Inspeção de props declaradas no Svelte e não enviadas pelo PHP (direção inversa) — descartada por gerar falso positivo com shared props/partial reloads.
- Para deferred/merge/once, tratar o wrapper (`Inertia::defer(...)`) como o tipo do prop, não como uma chave nova — a chave continua sendo a do array, então já funciona.

#### Inspeção opt-in (`InertiaUndefinedPagePropInspection`)

- Registrada em `plugin.xml` para `language="PHP"`, `enabledByDefault="false"`, `level="WEAK WARNING"`, grupo `Inertia.js`.
- Marca a chave do array de props quando o componente Svelte alvo não a lê do `$props()`.
- Não reporta nada (bail-out) quando: não é chamada Inertia; o array tem spread ou item sem chave string; a página não é `.svelte`; o componente não usa `$props()` (Svelte 4 `export let`); o destructuring tem `...rest`.
- Só é ligada manualmente em Settings → Inspections.

### Fase 5 — Testes ✅

20 testes, todos verdes (`./gradlew test`).

- `InertiaCallTest` (light) — reconhecimento de `inertia()` posicional/nomeado, `props` como argumento seguinte, e negativo para chamadas que não são Inertia. Usa só a função global porque ela é reconhecida pelo nome, sem depender do `PhpIndex`.
- `InertiaPropsExtractorTest` (light) — chaves do array PHP, itens sem chave, `parsePatternKeys` (alias, chave entre aspas, default, `...rest`) e leitura de `$props()` num arquivo `.svelte` real.
- `InertiaPageReferenceProviderTest` (light) — o contributor cria exatamente uma referência para o componente e ignora valores de props.
- `InertiaPageResolverTest` (heavy) — `resolve`/`allPages` contra um **content root real em disco**, com `ModuleRootManager.modifiableModel` + `addContentEntry` e `IndexingTestUtil.waitUntilIndexesAreReady`.

Aprendizados para novos testes:

- `BasePlatformTestCase` usa VFS em memória: `VfsUtil.findFileByIoFile` não vê esses arquivos. Para o resolver, use `HeavyPlatformTestCase` + diretório real registrado como content root.
- `Inertia::render()` e `Route::inertia()` exigem o `PhpIndex` resolvendo `Inertia\Inertia`/`Route`; por isso os testes de referência usam só `inertia()`.
- O plugin Svelte está em `platformPlugins` (`dev.blachut.svelte.lang:262.10968.63`) para que fixtures `.svelte` tenham PSI.

### Fase 6 — Verificação

- `./gradlew buildPlugin`
- `./gradlew check`
- `./gradlew verifyPlugin`
- Teste manual com `./gradlew runIde` em um projeto Laravel + Inertia + Svelte.

> Observação: `runIde` baixa e usa a distribuição PhpStorm 2026.2.4 configurada em `gradle.properties`. Para usar uma instalação local, troque `create(...)` por `local("<caminho da IDE>")` em `build.gradle.kts`.

## Referências

Consulte a documentação antes de implementar. Não invente APIs.

- Inertia 3.x (JS): https://github.com/inertiajs/inertia/tree/3.x
- Inertia v3 docs: https://inertiajs.com/docs/v3/getting-started
- Inertia docs index (LLM): https://inertiajs.com/docs/llms.txt
- Inertia Laravel: https://github.com/inertiajs/inertia-laravel
- Svelte: https://github.com/sveltejs/svelte
- Svelte 5 docs: https://svelte.dev/docs/svelte/overview
- IntelliJ Platform SDK: https://plugins.jetbrains.com/docs/intellij/
- IntelliJ Platform Gradle Plugin 2.x: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
- Svelte plugin (JetBrains): https://plugins.jetbrains.com/plugin/12375-svelte
- Svelte Hero: https://plugins.jetbrains.com/plugin/31546-svelte-hero

## Checklist de PR

- [ ] Baseline atualizada e build verde.
- [ ] Nenhuma API 2026 usada antes do bump da baseline.
- [ ] Dependência Svelte é opcional; plugin principal funciona sem ela.
- [ ] Índice usado em vez de varredura de VFS.
- [ ] Testes cobrindo resolve, completion e named args.
- [ ] `runPluginVerifier` sem erros.
- [ ] Documentação/skill atualizada se o comportamento mudar.
