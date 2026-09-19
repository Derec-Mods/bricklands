package com.alcatrazescapee.bricklands.util;

import java.util.Map;
import java.util.Optional;

import com.alcatrazescapee.bricklands.Bricklands;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Derived from HexLands by AlcatrazEscapee (MIT License), itself based on the
 * original Hex Lands (2019) by superfluke, anonlinux777, and TehNut.
 * Forked and adapted here for Bricklands.
 */
public record BrickSettings(double biomeScale, int widthChunks, int heightChunks, int rimSize, boolean mergeSameBiome, boolean randomBiomes, Optional<BorderSettings> topBorder, Optional<BorderSettings> bottomBorder)
{
    private static final Map<ResourceLocation, BrickSettings> DEFAULTS = new Object2ObjectOpenHashMap<>();
    private static final Codec<BrickSettings> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0.01, 1000).optionalFieldOf("biome_scale", 8d).forGetter(c -> c.biomeScale),
            Codec.intRange(1, 64).optionalFieldOf("width_chunks", 4).forGetter(c -> c.widthChunks),
            Codec.intRange(1, 64).optionalFieldOf("height_chunks", 2).forGetter(c -> c.heightChunks),
            Codec.intRange(0, 64).optionalFieldOf("rim_size", 2).forGetter(c -> c.rimSize),
            Codec.BOOL.optionalFieldOf("merge_same_biome", false).forGetter(c -> c.mergeSameBiome),
            Codec.BOOL.optionalFieldOf("random_biomes", false).forGetter(c -> c.randomBiomes),
            BorderSettings.CODEC.optionalFieldOf("top_border").forGetter(c -> c.topBorder),
            BorderSettings.CODEC.optionalFieldOf("bottom_border").forGetter(c -> c.bottomBorder)
    ).apply(instance, BrickSettings::new));

    public static final Codec<BrickSettings> CODEC = Codec.either(ResourceLocation.CODEC, DIRECT_CODEC).comapFlatMap(
        e -> e.map(
            l -> Optional.ofNullable(BrickSettings.DEFAULTS.get(l))
                .map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "No brick_settings named '" + l + "'")),
            DataResult::success),
        Either::right);

    static
    {
        register("overworld", new BrickSettings(32d, 4, 2, 2, false, false, Optional.empty(), BorderSettings.of(62, 74, Blocks.BRICKS)));
        register("nether", new BrickSettings(4d, 4, 2, 2, false, false, BorderSettings.of(100, 110, Blocks.NETHER_BRICKS), BorderSettings.of(31, 40, Blocks.NETHER_BRICKS)));
        register("the_end", new BrickSettings(4d, 4, 2, 2, false, false, Optional.empty(), Optional.empty()));
    }

    public int brickWidthBlocks()
    {
        return widthChunks * 16;
    }

    public int brickHeightBlocks()
    {
        return heightChunks * 16;
    }

    private static void register(String id, BrickSettings settings)
    {
        DEFAULTS.put(ResourceLocation.fromNamespaceAndPath(Bricklands.MOD_ID, id), settings);
    }

    public record BorderSettings(int minHeight, int maxHeight, BlockState state)
    {
        public static final Codec<BorderSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("min_height").forGetter(c -> c.minHeight),
            Codec.INT.fieldOf("max_height").forGetter(c -> c.maxHeight),
            BlockState.CODEC.fieldOf("state").forGetter(c -> c.state)
        ).apply(instance, BorderSettings::new));

        private static Optional<BorderSettings> of(int minHeight, int maxHeight, Block block)
        {
            return Optional.of(new BorderSettings(minHeight, maxHeight, block.defaultBlockState()));
        }

        public int sample(RandomSource random)
        {
            if (minHeight == maxHeight)
            {
                return minHeight;
            }
            return random.nextIntBetweenInclusive(minHeight, maxHeight);
        }
    }
}
