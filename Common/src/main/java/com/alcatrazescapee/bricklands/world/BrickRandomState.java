package com.alcatrazescapee.bricklands.world;

import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;
import com.alcatrazescapee.bricklands.mixin.RandomStateAccessor;
import com.alcatrazescapee.bricklands.platform.XPlatform;
import com.alcatrazescapee.bricklands.util.Brick;
import com.alcatrazescapee.bricklands.util.BrickSettings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Derived from HexLands by AlcatrazEscapee (MIT License), itself based on the
 * original Hex Lands (2019) by superfluke, anonlinux777, and TehNut.
 * Forked and adapted here for Bricklands.
 */
public record BrickRandomState(RandomState state, NoiseRouter brickRouter, Climate.Sampler brickSampler)
{
    private static final Cache<RandomState, BrickRandomState> RANDOM_STATE_EXTENSIONS = CacheBuilder.newBuilder()
        .concurrencyLevel(4)
        .weakKeys()
        .build();

    public static BrickRandomState modify(RandomState state, NoiseGeneratorSettings settings, BrickSettings brickSettings)
    {
        try
        {
            return RANDOM_STATE_EXTENSIONS.get(state, () -> {
                final DensityFunction.Visitor visitor = f -> {
                    if (isNoiseDensityFunction(f))
                    {
                        return sampleBrickRelative(brickSettings, f);
                    }
                    return f;
                };

                final NoiseRouter router = state.router();
                final NoiseRouter brickRouter = new NoiseRouter(
                    router.barrierNoise(),
                    router.fluidLevelFloodednessNoise(),
                    router.fluidLevelSpreadNoise(),
                    router.lavaNoise(),
                    sampleBrickCenter(brickSettings, router.temperature()),
                    sampleBrickCenter(brickSettings, router.vegetation()),
                    sampleBrickCenter(brickSettings, router.continents()),
                    sampleBrickCenter(brickSettings, router.erosion()),
                    sampleBrickCenter(brickSettings, router.depth()),
                    sampleBrickCenter(brickSettings, router.ridges()),
                    router.initialDensityWithoutJaggedness().mapAll(visitor),
                    router.finalDensity().mapAll(visitor),
                    router.veinToggle(),
                    router.veinRidged(),
                    router.veinGap()
                );

                final Climate.Sampler brickSampler = new Climate.Sampler(
                    brickRouter.temperature(),
                    brickRouter.vegetation(),
                    brickRouter.continents(),
                    brickRouter.erosion(),
                    brickRouter.depth(),
                    brickRouter.ridges(),
                    settings.spawnTarget()
                );

                XPlatform.INSTANCE.copyFabricCachedClimateSamplerSeed(state.sampler(), brickSampler);

                final RandomStateAccessor mutableState = (RandomStateAccessor) (Object) state;

                mutableState.setRouter(brickRouter);
                mutableState.setSampler(brickSampler);

                return new BrickRandomState(state, brickRouter, brickSampler);
            });
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException("Failed to inject BrickRandomState into RandomState", e);
        }
    }

    private static boolean isNoiseDensityFunction(DensityFunction f)
    {
        return f instanceof DensityFunctions.Noise || f instanceof DensityFunctions.Shift || f instanceof DensityFunctions.ShiftedNoise;
    }

    private static DensityFunction sampleBrickCenter(BrickSettings brickSettings, DensityFunction function)
    {
        return new PointMapped(function, function.minValue(), function.maxValue(), point -> {
            final double scale = brickSettings.biomeScale();
            final double width = brickSettings.brickWidthBlocks() * scale;
            final double height = brickSettings.brickHeightBlocks() * scale;
            final Brick brick = Brick.blockToBrick(point.blockX() * scale, point.blockZ() * scale, width, height);
            final BlockPos center = brick.center();

            return new DensityFunction.SinglePointContext(center.getX(), point.blockY(), center.getZ());
        });
    }

    private static DensityFunction sampleBrickRelative(BrickSettings brickSettings, DensityFunction function)
    {
        return new PointMapped(function, function.minValue(), function.maxValue(), point -> {
            final double scale = brickSettings.biomeScale();
            final double width = brickSettings.brickWidthBlocks();
            final double height = brickSettings.brickHeightBlocks();
            final Brick brick = Brick.blockToBrick(point.blockX() * scale, point.blockZ() * scale, width * scale, height * scale);
            final BlockPos center = brick.center();

            final double deltaX = point.blockX() - center.getX() / scale;
            final double deltaZ = point.blockZ() - center.getZ() / scale;

            return new DensityFunction.SinglePointContext(center.getX() + (int) deltaX, point.blockY(), center.getZ() + (int) deltaZ);
        });
    }

    record PointMapped(DensityFunction wrapped, double minValue, double maxValue, UnaryOperator<FunctionContext> point) implements DensityFunction.SimpleFunction
    {
        @Override
        public double compute(FunctionContext context)
        {
            return wrapped.compute(point.apply(context));
        }

        @Override
        public DensityFunction mapAll(Visitor visitor)
        {
            return new PointMapped(wrapped.mapAll(visitor), minValue, maxValue, point);
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec()
        {
            return KeyDispatchDataCodec.of(MapCodec.unit(this));
        }
    }
}
