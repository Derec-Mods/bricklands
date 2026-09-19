package com.alcatrazescapee.bricklands.mixin;

import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.alcatrazescapee.bricklands.Bricklands;
import com.alcatrazescapee.bricklands.client.BricklandsCustomizeScreen;

@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationUiStateMixin
{
    @Inject(method = "getPresetEditor", at = @At("RETURN"), cancellable = true)
    private void bricklands$enableCustomize(CallbackInfoReturnable<PresetEditor> cir)
    {
        if (cir.getReturnValue() != null)
        {
            return;
        }
        Holder<WorldPreset> preset = ((WorldCreationUiState) (Object) this).getWorldType().preset();
        if (preset != null && preset.unwrapKey().filter(key -> Bricklands.MOD_ID.equals(key.location().getNamespace())).isPresent())
        {
            cir.setReturnValue(BricklandsCustomizeScreen::create);
        }
    }
}
