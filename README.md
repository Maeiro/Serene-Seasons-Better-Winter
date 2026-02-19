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

`pass_through_hidden_blocks = true` allows entities to pass through hidden leaves and hidden snow layers above them.

## Leaf Customization Tutorial (Per Tree Type)

You can control exactly which leaves become invisible using datapack block tags.

Tags used by this mod:

- `serene_better_winter:deciduous_leaves`:
  - leaves in this tag are hidden during configured leafless seasons.
- `serene_better_winter:conifer_leaves`:
  - leaves in this tag are kept visible.

Priority rule:

- `conifer_leaves` wins over `deciduous_leaves` if a block is present in both.

Step-by-step:

1. Create a datapack folder in your world:
`<world>/datapacks/my_leaf_rules/`
2. Add `pack.mcmeta`:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "Custom leaf rules for Serene Seasons Better Winter"
  }
}
```

3. Create this path:
`<world>/datapacks/my_leaf_rules/data/serene_better_winter/tags/blocks/`
4. Create `deciduous_leaves.json` (blocks that should disappear):

```json
{
  "replace": false,
  "values": [
    "minecraft:oak_leaves",
    "minecraft:birch_leaves",
    "dynamic_trees:oak_leaves"
  ]
}
```

5. Create `conifer_leaves.json` (blocks that should stay visible):

```json
{
  "replace": false,
  "values": [
    "minecraft:spruce_leaves",
    "dynamic_trees:spruce_leaves"
  ]
}
```

6. Reload datapacks in game:
`/reload`

Tips:

- To force a specific leaf type to remain visible, add it to `conifer_leaves`.
- To force a specific leaf type to disappear, add it to `deciduous_leaves`.

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
  - make relight tuning configurable in `server.toml` (chunk cap and vertical scan window);
  - reduce `RELIGHT_CHUNK_LIMIT` (e.g. `900 -> 400/500`);
  - narrow relight vertical scan window (`topY - 20 .. topY + 4`);
  - add server-side cache for `CollisionRules.shouldHideLeafLike` by `Block`.
- Compatibility/behavior:
  - fix `remove_light_blocking_from_hidden_blocks` in non-Embeddium environments (vanilla/Forge renderer path).
  - improve light pass-through handling for hidden leaves in tall/very dense canopies when not using shaders (residual shadow still present even after ground-level leaf fixes).
- Feature/config:
  - add config to define which seasons/sub-seasons trigger leaf drop.
  - add optional chat message when entering a leaf-drop season/sub-season (configurable).
