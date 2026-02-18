# Serene Seasons Better Winter

Forge 1.20.1 addon that hides deciduous leaves in late autumn and winter.

## What It Does

- Works automatically after installing the mod.
- Adds a simple client config toggle for visuals.
- Targets deciduous leaves only.
- Keeps conifer and jungle leaves visible.
- Supports Serene Seasons season detection and Dynamic Trees fallback matching.
- Removes floating snow layer above hidden leaves.
- Optional server-side pass-through on hidden leaves/snow for better gameplay feel.

## Forge Version Policy

- Default dev/build target: `47.4.10`.
- Runtime compatibility declared in `mods.toml`: `[47.4.10,48)`.

## Install

1. Put the mod JAR in `mods/`.
2. Put `resourcepack/SereneBetterWinter-Assets` in `resourcepacks/` and enable it.
3. Start the game.

## Config

- File: `config/serene_better_winter-client.toml`
- Options:
  - `enabled = true` (default)
  - `hide_snow_above_hidden_leaves = true` (default)

If `enabled=false`, the mod does not hide any leaves.

- File: `world/serverconfig/serene_better_winter-server.toml`
- Options:
  - `pass_through_hidden_blocks = true` (default)
  - `remove_light_blocking_from_hidden_blocks = true` (default)
  - `force_relight_on_season_change = true` (default)

If `pass_through_hidden_blocks=true`, players/entities can pass through season-hidden leaves and snow layers hidden above those leaves.

## License

MIT

## TODO

- Performance tuning (server relight path):
  - Make relight tuning configurable in `server.toml` (chunk cap and vertical scan window).
  - Reduce `RELIGHT_CHUNK_LIMIT` (e.g. `900 -> 400/500`).
  - Narrow relight vertical scan window (`topY - 20 .. topY + 4`).
  - Add server-side cache for `CollisionRules.shouldHideLeafLike` by `Block`.
