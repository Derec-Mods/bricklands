package com.alcatrazescapee.bricklands;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Derived from HexLands by AlcatrazEscapee (MIT License), itself based on the
 * original Hex Lands (2019) by superfluke, anonlinux777, and TehNut.
 * Forked and adapted here for Bricklands.
 */
public final class FabricBricklands implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        Bricklands.init((id, e) -> Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id, e));
    }
}
