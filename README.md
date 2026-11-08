# Inertia.js Storm

PhpStorm and IntelliJ IDEA Ultimate plugin for [Inertia.js](https://inertiajs.com/).

![Build](https://github.com/gusantana/idea-inertiajs-plugin/workflows/Build/badge.svg)

<!-- Plugin description -->
Provides support in PhpStorm and IntelliJ IDEA Ultimate for [Inertia.js](https://inertiajs.com/):

- Navigation from `Inertia::render('Page')`, `inertia('Page')` and `Route::inertia('/url', 'Page')`, including PHP 8 named arguments
- Page name completion resolved from the configured pages root
- Gutter icon to find the PHP call sites of a page, on `.js`, `.jsx`, `.ts`, `.tsx`, `.vue` and `.svelte` files
- Prop key completion from the `$props()` of the target Svelte 5 component
- Opt-in inspection for props the Svelte page does not declare

Requires PhpStorm or IntelliJ IDEA Ultimate 2026.2.4 or newer (2026.2 and 2026.3 are supported). The Svelte plugin is optional and only needed for the `.svelte` features.
<!-- Plugin description end -->

## Installation

- **From disk**: download the latest `inertiajs-storm-*.zip` from the [releases](https://github.com/gusantana/idea-inertiajs-plugin/releases) and use *Settings | Plugins | gear | Install Plugin from Disk*.
- **Custom repository**: add `https://gusantana.github.io/idea-inertiajs-plugin/updatePlugins.xml` in *Settings | Plugins | gear | Manage Plugin Repositories*.
- **Marketplace**: once the listing is approved, install "Inertia.js Storm" from the Marketplace tab.

## Credits

- Forked from [hailwood/idea-inertiajs-plugin](https://github.com/hailwood/idea-inertiajs-plugin), ported to PhpStorm 2026.2 and extended with Inertia v3 and Svelte 5 support.
- [Inertia.js](https://inertiajs.com/) - The reason for this plugin
- [Robbin "Roboroads" Schepers](https://github.com/Roboroads) - Inspiration for plugin structure
