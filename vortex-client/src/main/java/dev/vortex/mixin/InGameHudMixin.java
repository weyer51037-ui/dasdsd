package dev.vortex.mixin;

import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void vortex$noVignette(CallbackInfo ci) {
        if (ModuleManager.isEnabled("no_vignette")) ci.cancel();
    }

    /** Hides the vanilla crosshair while the custom crosshair is on. */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void vortex$noCrosshair(CallbackInfo ci) {
        Module m = ModuleManager.get("crosshair");
        if (m != null && m.isEnabled() && m.getBool("Hide Vanilla")) ci.cancel();
    }
}
