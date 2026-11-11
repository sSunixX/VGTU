package dev.vgtu.render;

import dev.vgtu.VgtuClient;
import dev.vgtu.config.VgtuConfig;
import dev.vitrail.render.PackChain;
import dev.vitrail.render.RenderScale;
import dev.vitrail.render.TemporalAccumulation;
import net.minecraft.client.Minecraft;

/**
 * Direct adapter for the pinned Vitrail 0.13.0-beta+mc26.3 build.
 * Vitrail retains ownership of render targets, depth, reconstruction and presentation.
 */
public final class VitrailBridge {
    private VitrailBridge() {
    }

    public static void applyConfig() {
        setRenderScale(VgtuConfig.renderScale);
        if (!setTemporalAccumulation(VgtuConfig.temporalAccumulation)
                && VgtuConfig.temporalAccumulation) {
            VgtuClient.LOGGER.warn("VGTU could not apply Vitrail temporal accumulation; disabling the setting.");
            VgtuConfig.temporalAccumulation = false;
            VgtuConfig.save();
        }
    }

    public static void setRenderScale(int percent) {
        RenderScale.wanted(Math.max(25, Math.min(100, percent)));
    }

    /** Called by the frame-time sampler. The pinned Vitrail build exposes this as a public method. */
    public static boolean isShaderPackActive() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.level != null && PackChain.drawingPack();
    }

    /**
     * Vitrail's fold is not a full MEASU/FSR2 replacement. It is a temporal accumulation pass
     * that only runs while a shader pack is drawing and the world target is actually scaled.
     */
    public static boolean setTemporalAccumulation(boolean enabled) {
        try {
            TemporalAccumulation.setWanted(enabled);
            return true;
        } catch (RuntimeException | LinkageError exception) {
            VgtuClient.LOGGER.warn("Could not change Vitrail temporal accumulation.", exception);
            return false;
        }
    }

    public static boolean isTemporalAccumulationWanted() {
        try {
            return TemporalAccumulation.wanted();
        } catch (RuntimeException | LinkageError exception) {
            VgtuClient.LOGGER.warn("Could not read Vitrail temporal accumulation state.", exception);
            return false;
        }
    }
}
