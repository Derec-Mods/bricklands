package com.alcatrazescapee.bricklands.world;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

import com.alcatrazescapee.bricklands.util.Brick;
import com.alcatrazescapee.bricklands.util.BrickSettings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

/**
 * Derived from HexLands by AlcatrazEscapee (MIT License), itself based on the
 * original Hex Lands (2019) by superfluke, anonlinux777, and TehNut.
 * Forked and adapted here for Bricklands.
 */
public class BrickChunkGenerator extends NoiseBasedChunkGenerator
{
    public static final MapCodec<BrickChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(c -> c.directBiomeSource),
        NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(c -> c.settings),
        BrickSettings.CODEC.fieldOf("brick_settings").forGetter(c -> c.brickSettings)
    ).apply(instance, BrickChunkGenerator::new));

    private final BiomeSource directBiomeSource;
    private final Holder<NoiseGeneratorSettings> settings;
    private final BrickSettings brickSettings;

    private final Supplier<Aquifer.FluidPicker> stupidMojangGlobalFluidPicker;

    public BrickChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, BrickSettings brickSettings)
    {
        super(brickSettings.randomBiomes() ? new RandomBrickBiomeSource(biomeSource, brickSettings) : biomeSource, settings);
        this.directBiomeSource = biomeSource;
        this.settings = settings;
        this.brickSettings = brickSettings;

        this.stupidMojangGlobalFluidPicker = Suppliers.memoize(() -> {
            final NoiseGeneratorSettings noiseGeneratorSettings = settings.value();
            final Aquifer.FluidStatus lavaAtNeg54 = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
            final int seaLevel = noiseGeneratorSettings.seaLevel();
            final Aquifer.FluidStatus waterAtSeaLevel = new Aquifer.FluidStatus(seaLevel, noiseGeneratorSettings.defaultFluid());

            return (x, y, z) -> y < Math.min(-54, seaLevel) ? lavaAtNeg54 : waterAtSeaLevel;
        });
    }

    @Override
    protected MapCodec<BrickChunkGenerator> codec()
    {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess chunk)
    {
        super.buildSurface(level, structureManager, randomState, chunk);

        final NoiseChunk noiseChunk = getOrCreateNoiseChunk(chunk, randomState, structureManager, Blender.of(level));
        applyAtBrickBorders(chunk, randomState, noiseChunk, (cursor, placed) -> {

            // Bottom Border
            for (int y = placed.minY; y <= placed.borderMinY; y++)
            {
                final Block block = chunk.getBlockState(cursor.setY(y)).getBlock();
                if (block != Blocks.BEDROCK)
                {
                    chunk.setBlockState(cursor, placed.borderMinState, false);
                }
            }

            // Between Borders
            for (int y = placed.borderMinY + 1; y < placed.borderMaxY; y++)
            {
                chunk.setBlockState(cursor.setY(y), Blocks.AIR.defaultBlockState(), false);
            }

            // Top Border
            for (int y = placed.borderMaxY; y <= placed.maxY; y++)
            {
                final Block block = chunk.getBlockState(cursor.setY(y)).getBlock();
                if (block != Blocks.BEDROCK)
                {
                    chunk.setBlockState(cursor, placed.borderMaxState, false);
                }
            }
        });
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState state)
    {
        BrickRandomState.modify(state, settings.value(), brickSettings);
        return super.getBaseHeight(x, z, type, level, state);
    }

    @Override
    public void addDebugScreenInfo(List<String> tooltips, RandomState state, BlockPos pos)
    {
        final double brickScale = brickSettings.biomeScale();
        final double brickWidth = brickSettings.brickWidthBlocks() * brickScale;
        final double brickHeight = brickSettings.brickHeightBlocks() * brickScale;
        final Brick brick = Brick.blockToBrick(pos.getX() * brickScale, pos.getZ() * brickScale, brickWidth, brickHeight);
        final PlacedBrick placed = placeBrick(brick, state, null, pos.getY());

        tooltips.add(String.format("Brick (%d, %d) at %s : H%d B%d-%d", brick.col(), brick.row(), placed.biome().unwrap().map(ResourceKey::location, e -> "[unregistered biome]"), (int) placed.preliminaryHeight, placed.borderMinY, placed.borderMaxY));
        super.addDebugScreenInfo(tooltips, state, pos);
    }

    private void applyAtBrickBorders(ChunkAccess chunk, RandomState state, NoiseChunk noiseChunk, ColumnApplier applier)
    {
        final Map<Brick, PlacedBrick> cachedBiomesByBrick = new HashMap<>();
        final ChunkPos chunkPos = chunk.getPos();
        final int blockX = chunkPos.getMinBlockX(), blockZ = chunkPos.getMinBlockZ();

        final double brickScale = brickSettings.biomeScale();
        final double brickWidth = brickSettings.brickWidthBlocks() * brickScale;
        final double brickHeight = brickSettings.brickHeightBlocks() * brickScale;
        final double rimSize = brickSettings.rimSize() * brickScale;

        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; ++localX)
        {
            for (int localZ = 0; localZ < 16; ++localZ)
            {
                final int x = blockX + localX;
                final int z = blockZ + localZ;

                final Brick brick = Brick.blockToBrick(x * brickScale, z * brickScale, brickWidth, brickHeight);

                if (brick.distanceToEdge(x * brickScale, z * brickScale) <= rimSize)
                {
                    final PlacedBrick placed = cachedBiomesByBrick.computeIfAbsent(brick, k -> placeBrick(k, state, noiseChunk, 0));
                    if (brickSettings.mergeSameBiome())
                    {
                        final Brick adjacentBrick = brick.adjacent(x * brickScale, z * brickScale);
                        final PlacedBrick adjacentPlacedBrick = cachedBiomesByBrick.computeIfAbsent(adjacentBrick, k -> placeBrick(k, state, noiseChunk, 0));
                        if (placed.biome == adjacentPlacedBrick.biome)
                        {
                            continue;
                        }
                    }
                    cursor.setX(x).setZ(z);
                    applier.apply(cursor, placed);
                }
            }
        }
    }

    private PlacedBrick placeBrick(Brick brick, RandomState state, @Nullable NoiseChunk noiseChunk, int backupSurfaceY)
    {
        final BlockPos center = brick.center();
        final double brickScale = brickSettings.biomeScale();
        final int quartX = QuartPos.fromBlock((int) (center.getX() / brickScale));
        final int quartZ = QuartPos.fromBlock((int) (center.getZ() / brickScale));
        final NoiseSettings noiseSettings = settings.value().noiseSettings();
        final BrickRandomState brickRandomState = BrickRandomState.modify(state, settings.value(), brickSettings);
        final double preliminaryHeight = noiseChunk != null ? noiseChunk.preliminarySurfaceLevel((int) (center.getX() / brickScale), (int) (center.getZ() / brickScale)) : backupSurfaceY;
        final Holder<Biome> biome = getBiomeSource().getNoiseBiome(quartX, QuartPos.fromBlock((int) preliminaryHeight), quartZ, brickRandomState.brickSampler());
        final RandomSource random = new XoroshiroRandomSource(brick.col() * 178293412341L, brick.row() * 7520351231L);

        final int minY = noiseSettings.minY();
        final int maxY = noiseSettings.minY() + noiseSettings.height() - 1;

        final int borderMinY = brickSettings.bottomBorder()
            .map(border -> border.sample(random))
            .orElse(minY - 1);

        final int borderMaxY = brickSettings.topBorder()
            .map(border -> border.sample(random))
            .orElse(maxY + 1);

        final BlockState minBorderState = brickSettings.bottomBorder().map(BrickSettings.BorderSettings::state).orElse(Blocks.AIR.defaultBlockState());
        final BlockState maxBorderState = brickSettings.topBorder().map(BrickSettings.BorderSettings::state).orElse(Blocks.AIR.defaultBlockState());

        return new PlacedBrick(brick, biome, preliminaryHeight, minY, maxY, borderMinY, borderMaxY, minBorderState, maxBorderState);
    }

    private NoiseChunk getOrCreateNoiseChunk(ChunkAccess chunk, RandomState state, StructureManager structureManager, Blender blender)
    {
        return chunk.getOrCreateNoiseChunk(c -> NoiseChunk.forChunk(c, state, Beardifier.forStructuresInChunk(structureManager, c.getPos()), settings.value(), stupidMojangGlobalFluidPicker.get(), blender));
    }

    record PlacedBrick(Brick brick, Holder<Biome> biome, double preliminaryHeight, int minY, int maxY, int borderMinY, int borderMaxY, BlockState borderMinState, BlockState borderMaxState) {}

    @FunctionalInterface
    interface ColumnApplier
    {
        void apply(BlockPos.MutableBlockPos cursor, PlacedBrick placed);
    }

    /**
     * Picks one biome per brick from the parent source's biome list, seeded by brick column and row.
     */
    private static class RandomBrickBiomeSource extends BiomeSource
    {
        private final BiomeSource parent;
        private final BrickSettings brickSettings;
        private final List<Holder<Biome>> biomes;

        RandomBrickBiomeSource(BiomeSource parent, BrickSettings brickSettings)
        {
            this.parent = parent;
            this.brickSettings = brickSettings;
            this.biomes = parent.possibleBiomes().stream()
                .sorted(Comparator.comparing(holder -> holder.unwrapKey().map(key -> key.location().toString()).orElse("")))
                .toList();
        }

        @Override
        protected Stream<Holder<Biome>> collectPossibleBiomes()
        {
            return biomes.stream();
        }

        @Override
        protected MapCodec<? extends BiomeSource> codec()
        {
            return MapCodec.unit(this);
        }

        @Override
        public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler)
        {
            if (biomes.isEmpty())
            {
                return parent.getNoiseBiome(quartX, quartY, quartZ, sampler);
            }
            final double scale = brickSettings.biomeScale();
            final double width = brickSettings.brickWidthBlocks() * scale;
            final double height = brickSettings.brickHeightBlocks() * scale;
            final int blockX = QuartPos.toBlock(quartX);
            final int blockZ = QuartPos.toBlock(quartZ);
            final Brick brick = Brick.blockToBrick(blockX * scale, blockZ * scale, width, height);
            final RandomSource random = new XoroshiroRandomSource(brick.col() * 178293412341L, brick.row() * 7520351231L);
            return biomes.get(random.nextInt(biomes.size()));
        }
    }
}
