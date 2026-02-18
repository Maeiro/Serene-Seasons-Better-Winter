# Serene Seasons Better Winter

Forge 1.20.1 addon that hides deciduous leaves in late autumn and winter.

## What It Does

- Works automatically after installing the mod.
- Adds a single client config option: `enabled=true/false`.
- Targets deciduous leaves only.
- Keeps conifer leaves visible.
- Supports Serene Seasons season detection and Dynamic Trees fallback matching.

## Forge Version Policy

- Default dev/build target: `47.4.10`.
- Runtime compatibility declared in `mods.toml`: `[47.4.10,48)`.

## Install

1. Put the mod JAR in `mods/`.
2. Put `resourcepack/SereneBetterWinter-Assets` in `resourcepacks/` and enable it.
3. Start the game.

## Config

- File: `config/serene_better_winter-client.toml`
- Option:
  - `enabled = true` (default)
  - `hide_snow_above_hidden_leaves = true` (default)

If `enabled=false`, the mod does not hide any leaves.

## License

MIT
