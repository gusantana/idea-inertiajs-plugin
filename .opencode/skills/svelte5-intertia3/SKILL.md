---
name: Svelte 5 & InertiaJS v3 Plugin Development
description: Instruções reutilizáveis e diretrizes técnicas para implementar e refatorar suporte a Svelte 5 e InertiaJS v3 (inertia-laravel 3.x) em plugins Kotlin para PhpStorm 2026.2.4
---

# Skill: Desenvolvimento de Plugin PhpStorm para Svelte 5 & InertiaJS v3

Esta skill fornece instruções e fluxos de trabalho reutilizáveis para orientar agentes no desenvolvimento e refatoração de plugins do **PhpStorm 2026.2.4** voltados para o ecossistema **Svelte 5** e **InertiaJS v3** (`inertia-laravel` 3.x).

---

## 🛠️ Fluxo de Trabalho e Diretrizes Técnicas

### 1. Migração e Atualização de Plugins Legados (2023.1.3 → 2026.2.4)
1. **Configuração do Gradle**: Migre do modelo antigo DevKit ou IntelliJ Gradle Plugin 1.x para o **IntelliJ Platform Gradle Plugin 2.x**.
2. **Substituição de APIs Descontinuadas**:
   - Remova registros de `com.intellij.components` e migre para **Light Services** (`@Service` / `@Service(Service.Level.PROJECT)`).
   - Atualize chamadas de indexação PSI para utilizar as novas rotinas assíncronas da versão 2026.2.4.
3. **Descriptor de Módulos (Split Mode)**: Configure os arquivos de manifesto para suporte a desenvolvimento remoto (Remote Dev), separando APIs de Frontend e Backend quando aplicável.

### 2. Suporte ao Svelte 5 (Runes Architecture)
1. **Parser & Lexer de Svelte 5**:
   - Reconhecer as novas reatividades baseadas em Runes: `$state()`, `$derived()`, `$props()`, `$effect()`, `$inspect()`, `$host()`.
   - Garantir suporte à desestruturação de `$props()` e propriedades reativas em arquivos `.svelte`.
2. **Navegação e Autocompletar**:
   - Mapear referências entre componentes Svelte importados `<script>` e sua utilização no template.
   - Fornecer inspeção de tipos e autocomplete para props declaradas via `$props()`.

### 3. Suporte ao InertiaJS v3 & Laravel (`inertia-laravel` 3.x)
1. **Resolução de Chamadas em PHP**:
   - Identificar chamadas como `Inertia::render('Usuarios/Index', [...])` ou helpers `inertia('Usuarios/Index')` no PHP (via PhpStorm PHP OpenAPI).
   - Conectar o nome da página (`Usuarios/Index`) ao arquivo correspondente no diretório de visualizações frontend (ex: `resources/js/Pages/Usuarios/Index.svelte`).
2. **Autocompletar de Props entre Laravel e Svelte**:
   - Extrair chaves do array de dados passado no segundo argumento de `Inertia::render()` e sugerir preenchimento automático nas `$props()` do componente Svelte 5 alvo.
3. **Suporte a Novas Recursos do Inertia v3**:
   - Suportar navegação diferida (*deferred props*), mesclagem de props (*merge props*) e identificação de rotas nomeadas.

### 4. Regras de Desempenho e Threading na IntelliJ Platform
- **Operações Não Bloqueantes**: Toda análise de PSI PHP e Svelte deve ocorrer em threads de segundo plano sem bloquear a interface do usuário (EDT).
- **Invalidação de Cache de Índice**: Utilizar `StubIndex` ou `FileBasedIndex` para mapear rotas Inertia sem realizar leitura síncrona direta do disco rígido.
- **Serviços Sem Estado e Obtenção sob Demanda**: Garantir que as classes auxiliares de inspeção obtenham serviços via `project.service<InertiaProjectService>()` sem armazenar referências persistentes nos construtores.
