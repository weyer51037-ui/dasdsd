package dev.vortex.mixin;

import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void vortex$cull(Entity entity, Frustum frustum, double x, double y, double z,
                             CallbackInfoReturnable<Boolean> cir) {
        Module m = ModuleManager.get("entity_culling");
        if (m == null || !m.isEnabled()) return;
        if (entity == MinecraftClient.getInstance().player) return;
        if (entity instanceof PlayerEntity && m.getBool("Keep Players")) return;
        double max = m.getNum("Max Distance");
        if (entity.squaredDistanceTo(x, y, z) > max * max) cir.setReturnValue(false);
    }
}
