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
        entry("forest", "forest"),
        entry("flower_forest", "forest"),
        entry("birch_forest", "birch_forest"),
        entry("old_growth_birch_forest", "birch_forest"),
        entry("dark_forest", "dark_forest"),
        entry("pale_garden", "dark_forest"),
        entry("taiga", "taiga"),
        entry("old_growth_pine_taiga", "taiga"),
        entry("old_growth_spruce_taiga", "taiga"),
        entry("snowy_taiga", "snowy_taiga"),
        entry("swamp", "swamp"),
        entry("mangrove_swamp", "swamp"),
        entry("jungle", "jungle"),
        entry("sparse_jungle", "jungle"),
        entry("bamboo_jungle", "jungle"),
        entry("savanna", "savanna"),
        entry("savanna_plateau", "savanna"),
        entry("windswept_savanna", "savanna"),
        entry("badlands", "badlands"),
        entry("wooded_badlands", "badlands"),
        entry("eroded_badlands", "badlands"),
        entry("beach", "beach"),
        entry("snowy_beach", "beach"),
        entry("stony_shore", "beach"),
        entry("river", "river"),
        entry("frozen_river", "river"),
        entry("ocean", "ocean"),
        entry("deep_ocean", "ocean"),
        entry("cold_ocean", "ocean"),
        entry("deep_cold_ocean", "ocean"),
        entry("frozen_ocean", "ocean"),
        entry("deep_frozen_ocean", "ocean"),
        entry("lukewarm_ocean", "ocean"),
        entry("deep_lukewarm_ocean", "ocean"),
        entry("warm_ocean", "ocean"),
        entry("meadow", "meadow"),
        entry("cherry_grove", "meadow"),
        entry("grove", "slopes"),
        entry("snowy_slopes", "slopes"),
        entry("frozen_peaks", "peaks"),
        entry("jagged_peaks", "peaks"),
        entry("stony_peaks", "peaks"),
        entry("windswept_hills", "windswept"),
        entry("windswept_forest", "windswept"),
        entry("windswept_gravelly_hills", "windswept"),
        entry("dripstone_caves", "caves"),
        entry("lush_caves", "caves"),
        entry("deep_dark", "caves"),
        entry("crimson_forest", "nether_forest"),
        entry("warped_forest", "nether_forest"),
        entry("the_end", "the_end"),
        entry("end_highlands", "the_end"),
        entry("end_midlands", "the_end"),
        entry("end_barrens", "the_end"),
        entry("small_end_islands", "the_end")
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
