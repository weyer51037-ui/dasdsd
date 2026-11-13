package dev.vortex.mixin;

import dev.vortex.module.ModuleManager;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void vortex$noFire(CallbackInfo ci) {
        if (ModuleManager.isEnabled("no_fire")) ci.cancel();
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void vortex$noWater(CallbackInfo ci) {
        if (ModuleManager.isEnabled("no_water_overlay")) ci.cancel();
    }
}
