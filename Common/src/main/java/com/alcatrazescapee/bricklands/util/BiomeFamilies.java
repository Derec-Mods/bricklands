package com.alcatrazescapee.bricklands.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class BiomeFamilies
{
    private static final Map<String, String> VANILLA_FAMILIES = Map.ofEntries(
        entry("plains", "plains"),
        entry("sunflower_plains", "plains"),
        entry("snowy_plains", "snowy_plains"),
        entry("ice_spikes", "snowy_plains"),
        entry("birch_forest", "birch_forest"),
        entry("old_growth_birch_forest", "birch_forest"),
        entry("taiga", "taiga"),
        entry("old_growth_pine_taiga", "taiga"),
        entry("old_growth_spruce_taiga", "taiga"),
        entry("jungle", "jungle"),
        entry("sparse_jungle", "jungle"),
        entry("savanna", "savanna"),
        entry("savanna_plateau", "savanna"),
        entry("windswept_savanna", "savanna"),
        entry("badlands", "badlands"),
        entry("eroded_badlands", "badlands"),
        entry("river", "river"),
        entry("frozen_river", "river"),
        entry("ocean", "ocean"),
        entry("deep_ocean", "ocean"),
        entry("lukewarm_ocean", "lukewarm_ocean"),
        entry("deep_lukewarm_ocean", "lukewarm_ocean"),
        entry("cold_ocean", "cold_ocean"),
        entry("deep_cold_ocean", "cold_ocean"),
        entry("frozen_ocean", "frozen_ocean"),
        entry("deep_frozen_ocean", "frozen_ocean"),
        entry("windswept_hills", "windswept"),
        entry("windswept_forest", "windswept"),
        entry("windswept_gravelly_hills", "windswept"),
        entry("grove", "slopes"),
        entry("snowy_slopes", "slopes"),
        entry("frozen_peaks", "peaks"),
        entry("jagged_peaks", "peaks"),
        entry("stony_peaks", "peaks")
    );

    private BiomeFamilies() {}

    public static List<List<Holder<Biome>>> pools(Collection<Holder<Biome>> biomes)
    {
        Map<String, List<Holder<Biome>>> grouped = new TreeMap<>();
        for (Holder<Biome> biome : biomes)
        {
            grouped.computeIfAbsent(family(biome), key -> new ArrayList<>()).add(biome);
        }
        for (List<Holder<Biome>> pool : grouped.values())
        {
            pool.sort(Comparator.comparing(BiomeFamilies::biomeId));
        }
        return List.copyOf(grouped.values());
    }

    private static String family(Holder<Biome> biome)
    {
        return biome.unwrapKey()
            .map(key -> {
                ResourceLocation id = key.location();
                if ("minecraft".equals(id.getNamespace()))
                {
                    return VANILLA_FAMILIES.getOrDefault(id.getPath(), id.toString());
                }
                return id.toString();
            })
            .orElseGet(() -> biomeId(biome));
    }

    private static String biomeId(Holder<Biome> biome)
    {
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("");
    }

    private static Map.Entry<String, String> entry(String biome, String family)
    {
        return Map.entry(biome, family);
    }
}
