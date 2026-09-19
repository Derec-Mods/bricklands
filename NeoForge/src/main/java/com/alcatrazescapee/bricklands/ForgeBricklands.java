package com.alcatrazescapee.bricklands;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Derived from HexLands by AlcatrazEscapee (MIT License), itself based on the
 * original Hex Lands (2019) by superfluke, anonlinux777, and TehNut.
 * Forked and adapted here for Bricklands.
 */
@Mod(Bricklands.MOD_ID)
public final class ForgeBricklands
{
    private final DeferredRegister<MapCodec<? extends ChunkGenerator>> registry = DeferredRegister.create(BuiltInRegistries.CHUNK_GENERATOR, Bricklands.MOD_ID);

    public ForgeBricklands(IEventBus bus)
    {
        Bricklands.init((id, e) -> registry.register(id.getPath(), () -> e));
        registry.register(bus);
    }
}
