package dev.vgtu.mixin;

import dev.vgtu.performance.PerformanceGovernor;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererTimingMixin {
    @Unique
    private long vgtu$frameStartNanos;

    @Inject(method = "render", at = @At("HEAD"))
    private void vgtu$beginFrame(CallbackInfo ci) {
        this.vgtu$frameStartNanos = System.nanoTime();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void vgtu$endFrame(CallbackInfo ci) {
        long start = this.vgtu$frameStartNanos;
        if (start != 0L) {
            PerformanceGovernor.recordFrame(System.nanoTime() - start);
            this.vgtu$frameStartNanos = 0L;
        }
    }
}
