package com.alcatrazescapee.hexlands.world;

import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;
import com.alcatrazescapee.hexlands.mixin.RandomStateAccessor;
import com.alcatrazescapee.hexlands.platform.XPlatform;
import com.alcatrazescapee.hexlands.util.Brick;
import com.alcatrazescapee.hexlands.util.BrickSettings;
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

public record HexRandomState(RandomState state, NoiseRouter hexRouter, Climate.Sampler hexSampler)
{
    private static final Cache<RandomState, HexRandomState> RANDOM_STATE_EXTENSIONS = CacheBuilder.newBuilder()
        .concurrencyLevel(4)
        .weakKeys()
        .build();

    public static HexRandomState modify(RandomState state, NoiseGeneratorSettings settings, BrickSettings hexSettings)
    {
        try
        {
            return RANDOM_STATE_EXTENSIONS.get(state, () -> {
                final DensityFunction.Visitor visitor = f -> {
                    if (isNoiseDensityFunction(f))
                    {
                        return sampleHexRelative(hexSettings, f);
                    }
                    return f;
                };

                final NoiseRouter router = state.router();
                final NoiseRouter hexRouter = new NoiseRouter(
                    router.barrierNoise(),
                    router.fluidLevelFloodednessNoise(),
                    router.fluidLevelSpreadNoise(),
                    router.lavaNoise(),
                    sampleHexCenter(hexSettings, router.temperature()),
                    sampleHexCenter(hexSettings, router.vegetation()),
                    sampleHexCenter(hexSettings, router.continents()),
                    sampleHexCenter(hexSettings, router.erosion()),
                    sampleHexCenter(hexSettings, router.depth()),
                    sampleHexCenter(hexSettings, router.ridges()),
                    router.initialDensityWithoutJaggedness().mapAll(visitor),
                    router.finalDensity().mapAll(visitor),
                    router.veinToggle(),
                    router.veinRidged(),
                    router.veinGap()
                );

                final Climate.Sampler hexSampler = new Climate.Sampler(
                    hexRouter.temperature(),
                    hexRouter.vegetation(),
                    hexRouter.continents(),
                    hexRouter.erosion(),
                    hexRouter.depth(),
                    hexRouter.ridges(),
                    settings.spawnTarget()
                );

                XPlatform.INSTANCE.copyFabricCachedClimateSamplerSeed(state.sampler(), hexSampler);

                final RandomStateAccessor mutableState = (RandomStateAccessor) (Object) state;

                mutableState.setRouter(hexRouter);
                mutableState.setSampler(hexSampler);

                return new HexRandomState(state, hexRouter, hexSampler);
            });
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException("Failed to inject HexRandomState into RandomState", e);
        }
    }

    private static boolean isNoiseDensityFunction(DensityFunction f)
    {
        return f instanceof DensityFunctions.Noise || f instanceof DensityFunctions.Shift || f instanceof DensityFunctions.ShiftedNoise;
    }

    private static DensityFunction sampleHexCenter(BrickSettings hexSettings, DensityFunction function)
    {
        return new PointMapped(function, function.minValue(), function.maxValue(), point -> {
            final double scale = hexSettings.biomeScale();
            final double width = hexSettings.brickWidthBlocks() * scale;
            final double height = hexSettings.brickHeightBlocks() * scale;
            final Brick brick = Brick.blockToBrick(point.blockX() * scale, point.blockZ() * scale, width, height);
            final BlockPos center = brick.center();

            return new DensityFunction.SinglePointContext(center.getX(), point.blockY(), center.getZ());
        });
    }

    private static DensityFunction sampleHexRelative(BrickSettings hexSettings, DensityFunction function)
    {
        return new PointMapped(function, function.minValue(), function.maxValue(), point -> {
            final double scale = hexSettings.biomeScale();
            final double width = hexSettings.brickWidthBlocks();
            final double height = hexSettings.brickHeightBlocks();
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
