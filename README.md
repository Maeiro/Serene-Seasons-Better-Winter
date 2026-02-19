# Serene Seasons Better Winter

Forge `1.20.1` addon focused on seasonal immersion:
deciduous leaves disappear in late autumn/winter, while conifer and jungle leaves stay visible.

## Highlights

- Automatic behavior after install (no in-game activation flow).
- Works with Serene Seasons season state.
- Dynamic Trees-compatible leaf detection.
- Removes floating `minecraft:snow` layer above hidden leaves.
- Optional server gameplay mode:
  - pass through hidden leaves/snow collision;
  - remove light blocking from hidden blocks.

## Compatibility

- Minecraft: `1.20.1`
- Forge runtime range: `[47.4.10,48)`
- Primary dev target: `47.4.10`
- Required integration:
  - `sereneseasons`
- Optional integrations:
  - `dynamic_trees`
  - Embeddium (specialized renderer/light handling path)

## Installation

1. Put the mod jar in your `mods/` folder.
2. Copy `resourcepack/SereneBetterWinter-Assets` to `resourcepacks/`.
3. Enable the resource pack in-game.
4. Launch and play.

## Configuration

### Client

File: `config/serene_better_winter-client.toml`

- `enabled = true`
- `hide_snow_above_hidden_leaves = true`
- `hide_outline_for_hidden_blocks = true`

If `enabled = false`, the mod does not hide leaves or related snow visuals.

### Server

File: `world/serverconfig/serene_better_winter-server.toml`

- `pass_through_hidden_blocks = true`
- `remove_light_blocking_from_hidden_blocks = true`
- `force_relight_on_season_change = true`
- `relight_chunk_limit = 900`
- `relight_scan_below_top = 20`
- `relight_scan_above_top = 4`

`pass_through_hidden_blocks = true` allows entities to pass through hidden leaves and hidden snow layers above them.

## Build

```powershell
.\gradlew.bat clean build
```

Artifact output:

- `build/libs/serenebetterwinter-<modVersion>+forge-<mcVersion>.jar`

## Publishing Notes

- Mod metadata is declared in `src/main/resources/META-INF/mods.toml`.
- Mixin config is declared in both:
  - `src/main/resources/serene_better_winter.mixins.json`
  - jar manifest (`MixinConfigs` attribute).
- Project links:
  - Source: <https://github.com/Maeiro/Serene-Seasons-Better-Winter>
  - Issues: <https://github.com/Maeiro/Serene-Seasons-Better-Winter/issues>

## License

MIT (`LICENSE`)

## TODO

- Performance tuning (server relight path):
  - tune relight values per server profile using:
    - `relight_chunk_limit`
    - `relight_scan_below_top`
    - `relight_scan_above_top`
- Compatibility/behavior:
  - fix `remove_light_blocking_from_hidden_blocks` in non-Embeddium environments (vanilla/Forge renderer path).
  - improve light pass-through handling for hidden leaves in tall/very dense canopies when not using shaders (residual shadow still present even after ground-level leaf fixes).
- Feature/config:
  - add config to define which seasons/sub-seasons trigger leaf drop.
  - add optional chat message when entering a leaf-drop season/sub-season (configurable).
