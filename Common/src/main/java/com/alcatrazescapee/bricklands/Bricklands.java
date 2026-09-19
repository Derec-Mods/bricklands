package com.alcatrazescapee.bricklands;

import java.util.function.BiConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.slf4j.Logger;

import com.alcatrazescapee.bricklands.world.BrickChunkGenerator;

/**
 * Derived from HexLands by AlcatrazEscapee (MIT License), itself based on the
 * original Hex Lands (2019) by superfluke, anonlinux777, and TehNut.
 * Forked and adapted here for Bricklands.
 */
public final class Bricklands
{
    public static final String MOD_ID = "bricklands";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init(BiConsumer<ResourceLocation, MapCodec<? extends ChunkGenerator>> registry)
    {
        LOGGER.info("BOB THE BUILDER...");
        registry.accept(ResourceLocation.fromNamespaceAndPath(MOD_ID, "hexlands"), BrickChunkGenerator.CODEC);
        registry.accept(ResourceLocation.fromNamespaceAndPath(MOD_ID, "bricklands"), BrickChunkGenerator.CODEC);
    }
}
