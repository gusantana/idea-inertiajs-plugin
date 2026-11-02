# AGENTS.md

IntelliJ Platform plugin adding Inertia.js support to PhpStorm / IntelliJ IDEA Ultimate. Single Gradle module; all code is Kotlin under `src/main/kotlin`.

## Commands

- `./gradlew buildPlugin` — build the distributable zip. Also patches `plugin.xml` with version, since/until builds, the `README.md` description, and the `CHANGELOG.md` change notes.
- `./gradlew runIde` — launch a sandbox IDE with the plugin installed (logs at `build/idea-sandbox/system/log/idea.log`).
- `./gradlew check` — CI's verification task. There are currently **no tests** (`src/test` does not exist); when adding tests under `src/test/kotlin`, run one with `./gradlew test --tests "nz.hailwood.inertiajs.SomeTest"`.
- `./gradlew verifyPlugin` — run the IntelliJ Plugin Verifier against the IDEs configured in `intellijPlatform.pluginVerification`; downloads IDE distributions and is slow.
- `./gradlew cleanInspections runInspections` — Qodana inspections (requires Docker).
- `./gradlew patchChangelog --release-note="..."`, `signPlugin`, `publishPlugin` — release flow; require the env vars defined in `build.gradle.kts` (`PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`).
- `.run/*.xml` are IntelliJ run configs mirroring the above tasks.

## Build constraints

- `gradle.properties` is the source of truth for `pluginVersion`, `platformType` (PS), `platformVersion` (2026.2.4), `platformBundledPlugins`/`platformPlugins`, and `pluginSinceBuild`/`pluginUntilBuild`. Bump the version there and add a matching `CHANGELOG.md` entry — the build reads both.
- Toolchain: Gradle 9.1.0, Kotlin 2.4.21, IntelliJ Platform Gradle Plugin 2.19.0, JDK 21. IntelliJ Platform Gradle Plugin 2.x requires Gradle 9.0+.
- `build.gradle.kts` uses `jvmToolchain(21)`; a JDK 21 toolchain must be available (the CI workflows set up Java 21).
- The plugin requires the PHP, JavaScript and CSS platform plugins (`platformBundledPlugins` in `gradle.properties`) and imports `com.jetbrains.php.*` / JS PSI classes. It only works in PhpStorm or IDEA Ultimate.
- `intellijPlatform.pluginConfiguration.description` extracts the text between `<!-- Plugin description -->` and `<!-- Plugin description end -->` in `README.md` and fails if either marker is missing.
- `src/main/resources/META-INF/plugin.xml` registers extensions only; version and change notes are generated into the packaged copy, so don't hand-write them there.

## Svelte 5 / Inertia v3 roadmap

The baseline is PhpStorm 2026.2.4. Svelte 5 and Inertia v3 support are **not implemented yet**; the executable plan lives in the `svelte5-inertia3` skill. Read it before touching Svelte/Inertia features.

## Architecture

- Project detection lives in `Extensions.kt`: a project is Inertia if `composer.json` has `inertiajs/inertia-laravel` or `package.json` has an `@inertiajs/*` / `@inertia/*` package. `displayPath` converts a page file to its page name relative to the pages root.
- Pages root comes from `settings/InertiaSettingsService` (persisted as `.idea/inertia.xml`); default is `<project>/resources/js/Pages`. `InertiaStartupActivity` warns when that directory does not exist.
- `navigation/InertiaPageReferenceProvider` handles the three PHP call forms: `Inertia::render('Page')`, global `inertia('Page')`, and `Route::inertia('/url', 'Page')`. `InertiaPageReference` resolves/completes page names by walking files under the configured root.
- `InertiaRenderLineMarkerProvider` adds the "find usages" gutter icon on JS page files that are referenced from PHP.
- UI tests are run manually via `runIdeForUiTests` on port 8082 (see `.github/workflows/run-ui-tests.yml`); there is no local UI test suite.

## Working-copy gotcha

This checkout stores files with CRLF line endings, so `git diff` reports entire-file rewrites. Preserve CRLF when editing and do not commit unrelated line-ending churn.
