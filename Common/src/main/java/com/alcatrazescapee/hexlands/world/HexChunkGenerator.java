package com.alcatrazescapee.hexlands.world;

import java.util.*;
import java.util.function.Supplier;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

import com.alcatrazescapee.hexlands.util.Brick;
import com.alcatrazescapee.hexlands.util.BrickSettings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

public class HexChunkGenerator extends NoiseBasedChunkGenerator
{
    public static final MapCodec<HexChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(c -> c.biomeSource),
        NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(c -> c.settings),
        BrickSettings.CODEC.fieldOf("hex_settings").forGetter(c -> c.hexSettings)
    ).apply(instance, HexChunkGenerator::new));

    private final Holder<NoiseGeneratorSettings> settings;
    private final BrickSettings hexSettings;

    private final Supplier<Aquifer.FluidPicker> stupidMojangGlobalFluidPicker;

    public HexChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, BrickSettings hexSettings)
    {
        super(biomeSource, settings);
        this.settings = settings;
        this.hexSettings = hexSettings;

        this.stupidMojangGlobalFluidPicker = Suppliers.memoize(() -> {
            final NoiseGeneratorSettings noiseGeneratorSettings = settings.value();
            final Aquifer.FluidStatus lavaAtNeg54 = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
            final int seaLevel = noiseGeneratorSettings.seaLevel();
            final Aquifer.FluidStatus waterAtSeaLevel = new Aquifer.FluidStatus(seaLevel, noiseGeneratorSettings.defaultFluid());

            return (x, y, z) -> y < Math.min(-54, seaLevel) ? lavaAtNeg54 : waterAtSeaLevel;
        });
    }

    @Override
    protected MapCodec<HexChunkGenerator> codec()
    {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess chunk)
    {
        super.buildSurface(level, structureManager, randomState, chunk);

        final NoiseChunk noiseChunk = getOrCreateNoiseChunk(chunk, randomState, structureManager, Blender.of(level));
        applyAtHexBorders(chunk, randomState, noiseChunk, (cursor, placed) -> {

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
        HexRandomState.modify(state, settings.value(), hexSettings);
        return super.getBaseHeight(x, z, type, level, state);
    }

    @Override
    public void addDebugScreenInfo(List<String> tooltips, RandomState state, BlockPos pos)
    {
        final double hexScale = hexSettings.biomeScale();
        final double brickWidth = hexSettings.brickWidthBlocks() * hexScale;
        final double brickHeight = hexSettings.brickHeightBlocks() * hexScale;
        final Brick brick = Brick.blockToBrick(pos.getX() * hexScale, pos.getZ() * hexScale, brickWidth, brickHeight);
        final PlacedHex placed = placeHex(brick, state, null, pos.getY());

        tooltips.add(String.format("Brick (%d, %d) at %s : H%d B%d-%d", brick.col(), brick.row(), placed.biome().unwrap().map(ResourceKey::location, e -> "[unregistered biome]"), (int) placed.preliminaryHeight, placed.borderMinY, placed.borderMaxY));
        super.addDebugScreenInfo(tooltips, state, pos);
    }

    private void applyAtHexBorders(ChunkAccess chunk, RandomState state, NoiseChunk noiseChunk, ColumnApplier applier)
    {
        final Map<Brick, PlacedHex> cachedBiomesByBrick = new HashMap<>();
        final ChunkPos chunkPos = chunk.getPos();
        final int blockX = chunkPos.getMinBlockX(), blockZ = chunkPos.getMinBlockZ();

        final double hexScale = hexSettings.biomeScale();
        final double brickWidth = hexSettings.brickWidthBlocks() * hexScale;
        final double brickHeight = hexSettings.brickHeightBlocks() * hexScale;
        final double rimSize = hexSettings.rimSize() * hexScale;

        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; ++localX)
        {
            for (int localZ = 0; localZ < 16; ++localZ)
            {
                final int x = blockX + localX;
                final int z = blockZ + localZ;

                final Brick brick = Brick.blockToBrick(x * hexScale, z * hexScale, brickWidth, brickHeight);
                final Brick adjacentBrick = brick.adjacent(x * hexScale, z * hexScale);

                final PlacedHex placed = cachedBiomesByBrick.computeIfAbsent(brick, k -> placeHex(k, state, noiseChunk, 0));
                if (brick.distanceToEdge(x * hexScale, z * hexScale) <= rimSize)
                {
                    final PlacedHex adjacentPlacedHex = cachedBiomesByBrick.computeIfAbsent(adjacentBrick, k -> placeHex(k, state, noiseChunk, 0));
                    if (placed.biome != adjacentPlacedHex.biome)
                    {
                        cursor.setX(x).setZ(z);
                        applier.apply(cursor, placed);
                    }
                }
            }
        }
    }

    private PlacedHex placeHex(Brick brick, RandomState state, @Nullable NoiseChunk noiseChunk, int backupSurfaceY)
    {
        final BlockPos center = brick.center();
        final double hexScale = hexSettings.biomeScale();
        final int quartX = QuartPos.fromBlock((int) (center.getX() / hexScale));
        final int quartZ = QuartPos.fromBlock((int) (center.getZ() / hexScale));
        final NoiseSettings noiseSettings = settings.value().noiseSettings();
        final HexRandomState hexRandomState = HexRandomState.modify(state, settings.value(), hexSettings);
        final double preliminaryHeight = noiseChunk != null ? noiseChunk.preliminarySurfaceLevel((int) (center.getX() / hexScale), (int) (center.getZ() / hexScale)) : backupSurfaceY;
        final Holder<Biome> biome = biomeSource.getNoiseBiome(quartX, QuartPos.fromBlock((int) preliminaryHeight), quartZ, hexRandomState.hexSampler());
        final RandomSource random = new XoroshiroRandomSource(brick.col() * 178293412341L, brick.row() * 7520351231L);

        final int minY = noiseSettings.minY();
        final int maxY = noiseSettings.minY() + noiseSettings.height() - 1;

        final int borderMinY = hexSettings.bottomBorder()
            .map(border -> border.sample(random))
            .orElse(minY - 1);

        final int borderMaxY = hexSettings.topBorder()
            .map(border -> border.sample(random))
            .orElse(maxY + 1);

        final BlockState minBorderState = hexSettings.bottomBorder().map(BrickSettings.BorderSettings::state).orElse(Blocks.AIR.defaultBlockState());
        final BlockState maxBorderState = hexSettings.topBorder().map(BrickSettings.BorderSettings::state).orElse(Blocks.AIR.defaultBlockState());

        return new PlacedHex(brick, biome, preliminaryHeight, minY, maxY, borderMinY, borderMaxY, minBorderState, maxBorderState);
    }

    private NoiseChunk getOrCreateNoiseChunk(ChunkAccess chunk, RandomState state, StructureManager structureManager, Blender blender)
    {
        return chunk.getOrCreateNoiseChunk(c -> NoiseChunk.forChunk(c, state, Beardifier.forStructuresInChunk(structureManager, c.getPos()), settings.value(), stupidMojangGlobalFluidPicker.get(), blender));
    }

    record PlacedHex(Brick brick, Holder<Biome> biome, double preliminaryHeight, int minY, int maxY, int borderMinY, int borderMaxY, BlockState borderMinState, BlockState borderMaxState) {}

    @FunctionalInterface
    interface ColumnApplier
    {
        void apply(BlockPos.MutableBlockPos cursor, PlacedHex placed);
    }
}
