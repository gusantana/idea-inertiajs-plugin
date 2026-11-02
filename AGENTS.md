# AGENTS.md

IntelliJ Platform plugin adding Inertia.js support to PhpStorm / IntelliJ IDEA Ultimate. Single Gradle module; all code is Kotlin under `src/main/kotlin`.

## Commands

- `./gradlew buildPlugin` — build the distributable zip. Also runs `patchPluginXml`, which injects version, since/until builds, and changelog HTML into the packaged `plugin.xml`.
- `./gradlew runIde` — launch a sandbox IDE with the plugin installed (logs at `build/idea-sandbox/system/log/idea.log`).
- `./gradlew check` — CI's verification task. There are currently **no tests** (`src/test` does not exist); when adding tests under `src/test/kotlin`, run one with `./gradlew test --tests "nz.hailwood.inertiajs.SomeTest"`.
- `./gradlew runPluginVerifier` — verify against supported IDEs; downloads IDE distributions and is slow.
- `./gradlew cleanInspections runInspections` — Qodana inspections; set `QODANA_SHOW_REPORT=true` to open the report.
- `./gradlew patchChangelog --release-note="..."`, `signPlugin`, `publishPlugin` — release flow; require the env vars defined in `build.gradle.kts` (`PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`).
- `.run/*.xml` are IntelliJ run configs mirroring the above tasks.

## Build constraints

- `gradle.properties` is the source of truth for `pluginVersion`, `platformType` (IU), `platformVersion` (2022.3.1), and `pluginSinceBuild`/`pluginUntilBuild`. Bump the version there and add a matching `CHANGELOG.md` entry — the build reads both.
- Target the 2022.3 platform APIs; the toolchain is intentionally old (Gradle 7.6, Kotlin 1.8.0, Gradle IntelliJ plugin 1.12.0). Do not introduce newer-platform APIs or a newer Kotlin version without updating the platform baseline.
- `build.gradle.kts` uses `jvmToolchain(17)`; a JDK 17 toolchain must be available (the CI workflows still set up Java 11, so don't copy their JDK version).
- The plugin requires the PHP and JavaScript platform plugins (`platformPlugins` in `gradle.properties`) and imports `com.jetbrains.php.*` / JS PSI classes. It only works in PhpStorm or IDEA Ultimate.
- `patchPluginXml` extracts the text between `<!-- Plugin description -->` and `<!-- Plugin description end -->` in `README.md` and fails if either marker is missing.
- `src/main/resources/META-INF/plugin.xml` registers extensions only; version and change notes are generated into the packaged copy, so don't hand-write them there.

## Architecture

- Project detection lives in `Extensions.kt`: a project is Inertia if `composer.json` has `inertiajs/inertia-laravel` or `package.json` has an `@inertiajs/*` / `@inertia/*` package. `displayPath` converts a page file to its page name relative to the pages root.
- Pages root comes from `settings/InertiaSettingsService` (persisted as `.idea/inertia.xml`); default is `<project>/resources/js/Pages`. `InertiaStartupActivity` warns when that directory does not exist.
- `navigation/InertiaPageReferenceProvider` handles the three PHP call forms: `Inertia::render('Page')`, global `inertia('Page')`, and `Route::inertia('/url', 'Page')`. `InertiaPageReference` resolves/completes page names by walking files under the configured root.
- `InertiaRenderLineMarkerProvider` adds the "find usages" gutter icon on JS page files that are referenced from PHP.
- UI tests are run manually via `runIdeForUiTests` on port 8082 (see `.github/workflows/run-ui-tests.yml`); there is no local UI test suite.

## Working-copy gotcha

This checkout stores files with CRLF line endings, so `git diff` reports entire-file rewrites. Preserve CRLF when editing and do not commit unrelated line-ending churn.
