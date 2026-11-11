package dev.vgtu.render;

import dev.vgtu.VgtuClient;
import dev.vgtu.config.VgtuConfig;
import dev.vitrail.render.RenderScale;
import net.minecraft.client.Minecraft;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Adapter for Vitrail 0.13.0-beta+mc26.3.
 *
 * Vitrail owns the render-target swap, depth, spatial reconstruction, and presentation.
 * VGTU adjusts that system rather than creating a competing second render-scale pipeline.
 */
public final class VitrailBridge {
    private static final String PACK_CHAIN_CLASS = "dev.vitrail.render.PackChain";
    private static final String TEMPORAL_CLASS = "dev.vitrail.render.TemporalAccumulation";

    private static boolean warnedPackProbe;

    private VitrailBridge() {
    }

    public static void applyConfig() {
        setRenderScale(VgtuConfig.renderScale);

        if (!setTemporalAccumulation(VgtuConfig.temporalAccumulation)
                && VgtuConfig.temporalAccumulation) {
            VgtuClient.LOGGER.warn("The installed Vitrail build does not expose its optional "
                    + "temporal-accumulation hook. VGTU is leaving it disabled.");
            VgtuConfig.temporalAccumulation = false;
            VgtuConfig.save();
        }
    }

    public static void setRenderScale(int percent) {
        RenderScale.wanted(Math.max(25, Math.min(100, percent)));
    }

    /**
     * True only while a level is open and Vitrail has an active shader pack.
     * This optional probe avoids changing render scale in the menu or without a shader pack.
     */
    public static boolean isShaderPackActive() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return false;
        }

        try {
            Class<?> type = Class.forName(PACK_CHAIN_CLASS, true,
                    VitrailBridge.class.getClassLoader());
            Method method = type.getMethod("drawingPack");
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            if (!warnedPackProbe) {
                warnedPackProbe = true;
                VgtuClient.LOGGER.warn("VGTU could not detect the active Vitrail shader pack. "
                        + "Automatic scaling is paused for this Vitrail build.");
            }
            return false;
        } catch (InvocationTargetException exception) {
            VgtuClient.LOGGER.warn("Vitrail's shader-pack state probe failed.",
                    exception.getCause());
            return false;
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!warnedPackProbe) {
                warnedPackProbe = true;
                VgtuClient.LOGGER.warn("VGTU could not read Vitrail's shader-pack state.",
                        exception);
            }
            return false;
        }
    }

    /**
     * Optional reflective bridge. TemporalAccumulation is present in Vitrail's current source,
     * but it is not treated as a stable public API of every released Vitrail build.
     */
    public static boolean setTemporalAccumulation(boolean enabled) {
        try {
            Class<?> type = Class.forName(TEMPORAL_CLASS, true,
                    VitrailBridge.class.getClassLoader());
            Method method = type.getMethod("setWanted", boolean.class);
            method.invoke(null, enabled);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            return !enabled;
        } catch (InvocationTargetException exception) {
            VgtuClient.LOGGER.warn("Vitrail rejected the temporal-accumulation setting.",
                    exception.getCause());
            return false;
        } catch (ReflectiveOperationException | LinkageError exception) {
            VgtuClient.LOGGER.warn("Could not access Vitrail's optional temporal-accumulation hook.",
                    exception);
            return false;
        }
    }
}
