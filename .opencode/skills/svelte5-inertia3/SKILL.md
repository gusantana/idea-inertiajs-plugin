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

- Inspeção de props entre PHP e Svelte 5 (o completion de chaves de props já existe).
- Tratamento das features do Inertia v3 que mudam props (deferred, once, merge, optional, always) em inspeções.
- Testes desabilitados: `src/test/kotlin/.../*Test.kt` existe, mas as classes estão `@Ignore` até a Fase 5 ser validada.

Não descreva essas features como prontas. Se precisar delas, implemente.

## Alvo do port

- PhpStorm 2026.2.4 / IntelliJ Platform 2026.2.
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

- `platformType = PS`, `platformVersion = 2026.2.4` (build `262.11879.104`).
- `pluginSinceBuild = 262`, `pluginUntilBuild = 262.*`.
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

- Inspeção de props declaradas no PHP e não usadas no Svelte (ou vice-versa). Cuidado com ruído: props compartilhadas, partial reloads e `$page.props` tornam o warning propenso a falso positivo.
- Para deferred/merge/once, tratar o wrapper (`Inertia::defer(...)`) como o tipo do prop, não como uma chave nova — a chave continua sendo a do array, então já funciona; falta apenas usar isso em uma inspeção.

### Fase 5 — Testes ⏸️ (adiada)

Existem testes escritos, porém **desabilitados** com `@Ignore` até serem validados:

- `src/test/kotlin/nz/hailwood/inertiajs/navigation/InertiaPageResolverTest.kt` — `resolve` e `allPages`.
- `src/test/kotlin/nz/hailwood/inertiajs/navigation/InertiaPageReferenceProviderTest.kt` — `Inertia::render` posicional/nomeado, `inertia()` e `Route::inertia` posicional/nomeado, e o caso negativo da URI.

Para habilitar:

- Remova o `@Ignore`, rode `./gradlew test` e ajuste os stubs PHP se o `PhpIndex` não resolver `Inertia\Inertia`/`Route` no fixture.
- Use `IndexingTestUtil.waitUntilIndexesAreReady()` após criar arquivos (já usado nos testes).
- **Atenção:** `BasePlatformTestCase.addFileToProject` cria arquivos num VFS temporário em memória. `InertiaPageResolver.resolve` usa `VfsUtil.findFileByIoFile`, que só enxerga arquivos em disco — por isso os testes do resolver falharam. Use `myFixture.tempDirFixture` (diretório real em disco) ou `TempDirTestFixture` em vez de `addFileToProject`.
- Para o line marker em `.svelte`, é preciso adicionar o plugin Svelte ao classpath de teste (`plugin("dev.blachut.svelte.lang:262.10968.63")`).

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
