package dev.vortex.mixin;

import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void vortex$limit(CallbackInfoReturnable<Particle> cir) {
        Module m = ModuleManager.get("particle_limiter");
        if (m != null && m.isEnabled() && Math.random() * 100.0 >= m.getNum("Keep Percent")) {
            cir.setReturnValue(null);
        }
    }
}
