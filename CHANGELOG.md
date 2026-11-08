# Changelog
All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.1] - 2026-11-08
### Added
- Support for PhpStorm and IntelliJ IDEA 2026.3 (`since-build` 262, `until-build` 263.*)
- Plugin Verifier now checks both 2026.2.4 and 2026.3 for PhpStorm and IntelliJ IDEA Ultimate

## [0.2.0] - 2026-11-02
### Added
- Support for PHP 8 named arguments in `Inertia::render()`, `inertia()` and `Route::inertia()`
- Prop key completion from the `$props()` of the target Svelte 5 page
- Opt-in inspection for props that the target Svelte page does not declare in `$props()`
- Gutter icon on Vue and Svelte page files (Svelte requires the Svelte plugin, optional dependency)
- Test suite for the Inertia call matching, props extraction, page references and page resolution

### Changed
- Renamed to **Inertia.js Storm** with the plugin ID `io.github.gusantana.inertiajs`; installs of the previous ID are not updated automatically
- Ported to PhpStorm 2026.2.4 (262.*), IntelliJ Platform Gradle Plugin 2.19, Kotlin 2.4 and JDK 21
- Page resolution no longer scans the pages directory on every lookup; completion now uses `FilenameIndex`
- Dropped deprecated IntelliJ Platform APIs and the internal PHP `ParameterList.getParameter(String, int)`
- Support for 2023.1 EAP

## [0.1.7]

### Added
- Support for new Inertia 1.0 package names

## [0.1.6]

### Changed
- Support for 2022.3 Preview
- Sync with latest plugin template

## [0.1.5]

### Changed
- Support for EAP 2022.2

## [0.1.4]

### Changed
- Support for EAP 2022.1

## [0.1.2]

### Fixed
- Completion results on Windows no longer show full path [#11](https://github.com/hailwood/idea-inertiajs-plugin/issues/11)

## [0.1.1]

### Added
- Notification when Inertia root does not exist
- Route::inertia support [#6](https://github.com/hailwood/idea-inertiajs-plugin/issues/6)

### Changed
- Inertia project detection now looks for `@inertiajs/inertia` in package.json or `inertiajs/inertia-laravel` in composer.json

## [0.0.1]

### Added
- Go to reference support on Inertia::render and inertia() calls
- Go to usages line marker on first line of Inertia pages
- Settings page to set where Inertia pages are stored

[Unreleased]: https://github.com/hailwood/idea-inertiajs-plugin/compare/v0.1.7...HEAD
[0.1.7]: https://github.com/hailwood/idea-inertiajs-plugin/compare/v0.1.6...v0.1.7
[0.1.6]: https://github.com/hailwood/idea-inertiajs-plugin/compare/v0.1.5...v0.1.6
[0.1.5]: https://github.com/hailwood/idea-inertiajs-plugin/compare/v0.1.4...v0.1.5
[0.1.4]: https://github.com/hailwood/idea-inertiajs-plugin/compare/v0.1.2...v0.1.4
[0.1.2]: https://github.com/hailwood/idea-inertiajs-plugin/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/hailwood/idea-inertiajs-plugin/compare/v0.0.1...v0.1.1
[0.0.1]: https://github.com/hailwood/idea-inertiajs-plugin/commits/v0.0.1
