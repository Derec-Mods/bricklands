package com.alcatrazescapee.bricklands.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

import com.alcatrazescapee.bricklands.util.BrickSettings;
import com.alcatrazescapee.bricklands.world.BrickChunkGenerator;

/**
 * World-create Customize screen hooked into Minecraft's built-in Customize button.
 */
public class BricklandsCustomizeScreen extends Screen
{
    private static final Component TITLE = Component.translatable("bricklands.customize.title");

    private final CreateWorldScreen parent;
    private double biomeScale;
    private int widthChunks;
    private int heightChunks;
    private int rimSize;
    private boolean mergeSameBiome;
    private boolean randomBiomes;

    public static Screen create(CreateWorldScreen parent, WorldCreationContext context)
    {
        return new BricklandsCustomizeScreen(parent, context);
    }

    private BricklandsCustomizeScreen(CreateWorldScreen parent, WorldCreationContext context)
    {
        super(TITLE);
        this.parent = parent;

        BrickSettings settings = BrickSettings.overworldDefaults();
        ChunkGenerator overworld = context.selectedDimensions().overworld();
        if (overworld instanceof BrickChunkGenerator brick)
        {
            settings = brick.brickSettings();
        }
        this.biomeScale = settings.biomeScale();
        this.widthChunks = settings.widthChunks();
        this.heightChunks = settings.heightChunks();
        this.rimSize = settings.rimSize();
        this.mergeSameBiome = settings.mergeSameBiome();
        this.randomBiomes = settings.randomBiomes();
    }

    @Override
    protected void init()
    {
        int width = 310;
        int x = this.width / 2 - width / 2;
        int y = 40;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.randomBiomes)
            .create(x, y, width, 20, Component.translatable("bricklands.customize.random_biomes"), (button, value) -> this.randomBiomes = value));
        y += 24;
        this.addRenderableWidget(CycleButton.onOffBuilder(this.mergeSameBiome)
            .create(x, y, width, 20, Component.translatable("bricklands.customize.merge_same_biome"), (button, value) -> this.mergeSameBiome = value));
        y += 24;
        this.addRenderableWidget(CycleButton.<Integer>builder(value -> Component.literal(value.toString()))
            .withValues(withCurrent(this.widthChunks, 1, 2, 4, 8, 16))
            .withInitialValue(this.widthChunks)
            .create(x, y, width, 20, Component.translatable("bricklands.customize.width_chunks"), (button, value) -> this.widthChunks = value));
        y += 24;
        this.addRenderableWidget(CycleButton.<Integer>builder(value -> Component.literal(value.toString()))
            .withValues(withCurrent(this.heightChunks, 1, 2, 4, 8))
            .withInitialValue(this.heightChunks)
            .create(x, y, width, 20, Component.translatable("bricklands.customize.height_chunks"), (button, value) -> this.heightChunks = value));
        y += 24;
        this.addRenderableWidget(CycleButton.<Integer>builder(value -> Component.literal(value.toString()))
            .withValues(withCurrent(this.rimSize, 0, 1, 2, 4, 8))
            .withInitialValue(this.rimSize)
            .create(x, y, width, 20, Component.translatable("bricklands.customize.rim_size"), (button, value) -> this.rimSize = value));
        y += 24;
        int scale = (int) Math.round(this.biomeScale);
        this.addRenderableWidget(CycleButton.<Integer>builder(value -> Component.literal(value.toString()))
            .withValues(withCurrent(scale, 4, 8, 16, 32, 64))
            .withInitialValue(scale)
            .create(x, y, width, 20, Component.translatable("bricklands.customize.biome_scale"), (button, value) -> this.biomeScale = value));

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onDone())
            .bounds(this.width / 2 - 155, this.height - 28, 150, 20)
            .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
            .bounds(this.width / 2 + 5, this.height - 28, 150, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void onClose()
    {
        this.minecraft.setScreen(this.parent);
    }

    private void onDone()
    {
        this.parent.getUiState().updateDimensions(this::updateDimensions);
        this.minecraft.setScreen(this.parent);
    }

    private WorldDimensions updateDimensions(RegistryAccess.Frozen registries, WorldDimensions dimensions)
    {
        Map<ResourceKey<LevelStem>, LevelStem> next = new LinkedHashMap<>();
        for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : dimensions.dimensions().entrySet())
        {
            LevelStem stem = entry.getValue();
            if (stem.generator() instanceof BrickChunkGenerator brick)
            {
                BrickSettings updated = brick.brickSettings().withCustomize(this.biomeScale, this.widthChunks, this.heightChunks, this.rimSize, this.mergeSameBiome, this.randomBiomes);
                next.put(entry.getKey(), new LevelStem(stem.type(), brick.withBrickSettings(updated)));
            }
            else
            {
                next.put(entry.getKey(), stem);
            }
        }
        return new WorldDimensions(next);
    }

    @SafeVarargs
    private static List<Integer> withCurrent(int current, Integer... values)
    {
        List<Integer> options = new ArrayList<>(List.of(values));
        if (!options.contains(current))
        {
            options.add(0, current);
        }
        return options;
    }
}
