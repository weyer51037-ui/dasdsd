package dev.vortex.mixin;

import dev.vortex.feature.ZoomFeature;
import dev.vortex.module.ModuleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void vortex$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (changingFov && ModuleManager.isEnabled("zoom")) {
            cir.setReturnValue(ZoomFeature.applyFov(cir.getReturnValue()));
        }
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void vortex$noHurtCam(CallbackInfo ci) {
        if (ModuleManager.isEnabled("no_hurt_cam")) ci.cancel();
    }

    /** This is the menu background blur. Cancelling it removes the blur from every screen. */
    @Inject(method = "renderBlur", at = @At("HEAD"), cancellable = true)
    private void vortex$noBlur(CallbackInfo ci) {
        if (ModuleManager.isEnabled("no_menu_blur")) ci.cancel();
    }
}
