This mod is based on [HexLands](https://github.com/alcatrazescapee/hexlands) by AlcatrazEscapee (MIT), rewritten so biomes sit on a staggered brick grid instead of hexes. HexLands itself is based on the original [Hex Lands](https://github.com/superfluke/hexlands) (2019) by superfluke, anonlinux777, and TehNut.

### Features

- Adds two world type presets: "Bricklands", and "Bricklands (Overworld)". The former enables brick terrain generation in the overworld and the nether; the latter only enables it in the overworld. The world create **Customize** button edits brick size, rims, merge-same-biome, and random biomes.
- Each brick is 4 chunks wide by 2 chunks tall, with odd rows shifted by half a brick. Each brick contains a single biome. Bricks of different types are bordered by walls.
- Automatic compatibility with mods that add biomes to the overworld or other world generation.
- Many options for world customization via data packs.

### Configuration (Data Packs - 1.21)

Worlds are specified by [World Presets](https://minecraft.wiki/w/World_preset). Bricklands can be customized by adding a new world preset, which uses the `bricklands:bricklands` chunk generator. The generator has the following fields:

- `type` is a string identifying what generator to use. It should be `bricklands:bricklands`. The older `bricklands:hexlands` id still works.
- `settings` is a [Noise Settings](https://minecraft.wiki/w/Noise_settings) used by the dimension.
- `biome_source` is the biome source, as in vanilla. It can be a known preset, such as `"minecraft:overworld"`, or `"minecraft:nether"`, or it can be a JSON object following the vanilla biome source format.
- `brick_settings` is an object with parameters defining how the brick grid works. It can either be a known preset, which must be one of `"bricklands:overworld"`, `"bricklands:nether"`, or `"bricklands:the_end"`, or it can be an object with the following fields:
    - `biome_scale` (Default: 8) is the scale at which biomes are sampled to create bricks. Higher values create more random biome layouts.
    - `width_chunks` (Default: 4) is the width of an individual brick, in chunks.
    - `height_chunks` (Default: 2) is the height of an individual brick, in chunks. Odd rows are shifted by half this width.
    - `rim_size` (Default: 2) is how many blocks thick the border is, measured inward from the brick edge.
    - `merge_same_biome` (Default: false) when true, skips rims between neighboring bricks that share a biome so they merge. When false, every brick edge gets a rim.
    - `random_biomes` (Default: false) when true, each brick picks a biome at random from that dimension's biome list instead of sampling climate.
    - `top_border` and `bottom_border` are both border settings which define how the top and bottom borders of the world are built. The borders between bricks consist of a bottom border, air, and a top border. If not present, this section of the border will consist entirely of air. If present, it must have the following fields:
        - `min_height`: The minimum height of the border.
        - `max_height`: The maximum height of the border.
        - `state`: A block state to generate as the border state. It must be an object with the following fields:
          - `Name`: The name of the block
          - `Properties`: An object with any block state properties, such as `{"snowy": "false"}` that you desire to set

**Example**

```json5
// Below is an example object which can be used in the `generator` field of a world preset.
{
    "type": "bricklands:bricklands",
    "settings": "minecraft:overworld",
    "biome_source": {
      "type": "minecraft:multi_noise",
      "preset": "minecraft:overworld"
    },
    "brick_settings": "bricklands:overworld"
}
```

### Attribution

- [Hex Lands](https://github.com/superfluke/hexlands) (2019) by superfluke, anonlinux777, and TehNut
- [HexLands](https://github.com/alcatrazescapee/hexlands) by AlcatrazEscapee (MIT License)
