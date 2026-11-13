package dev.vortex.mixin;

import dev.vortex.feature.FreelookFeature;
import dev.vortex.feature.ZoomFeature;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void vortex$scroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ZoomFeature.onScroll(vertical)) ci.cancel();
    }

    @Redirect(method = "updateMouse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void vortex$look(ClientPlayerEntity player, double dx, double dy) {
        if (FreelookFeature.handle(dx, dy)) return;
        player.changeLookDirection(dx, dy);
    }
}
