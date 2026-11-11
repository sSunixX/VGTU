package dev.vgtu.performance;

import dev.vgtu.VgtuClient;
import dev.vgtu.config.VgtuConfig;
import dev.vgtu.render.VitrailBridge;

import java.util.Arrays;

/**
 * Conservative dynamic-resolution controller.
 *
 * Samples GameRenderer.render() wall time, not pure GPU time. CPU work, driver waits and VSync
 * can affect it, so the controller uses hysteresis and verifies that a downscale helped.
 */
public final class PerformanceGovernor {
    private static final int WINDOW_SIZE = 120;
    private static final int EVALUATE_EVERY_FRAMES = 30;
    private static final int CHANGE_COOLDOWN_FRAMES = 180;
    private static final int PROBE_FRAMES = 180;

    private static final long[] FRAME_TIMES = new long[WINDOW_SIZE];
    // Reused by percentile90Ms() to avoid allocating a new array on each controller evaluation.
    private static final long[] SORT_BUFFER = new long[WINDOW_SIZE];

    private static int windowSize;
    private static int nextIndex;
    private static long totalFrames;
    private static long lastEvaluationFrame;
    private static long lastScaleChangeFrame;

    private static int overBudgetWindows;
    private static int underBudgetWindows;

    private static boolean probePending;
    private static long probeStartFrame;
    private static double probeBeforeMs;
    private static int noGainSteps;
    private static boolean likelyCpuOrPresentBound;
    private static int baselineScale = 100;

    private PerformanceGovernor() {
    }

    public static synchronized void initialize() {
        baselineScale = VgtuConfig.renderScale;
        resetControllerState();
    }

    public static synchronized void recordFrame(long frameNanos) {
        if (!VitrailBridge.isShaderPackActive()) {
            windowSize = 0;
            nextIndex = 0;
            overBudgetWindows = 0;
            underBudgetWindows = 0;
            probePending = false;
            return;
        }

        if (frameNanos <= 0L || frameNanos > 2_000_000_000L) {
            return;
        }

        FRAME_TIMES[nextIndex] = frameNanos;
        nextIndex = (nextIndex + 1) % WINDOW_SIZE;
        windowSize = Math.min(windowSize + 1, WINDOW_SIZE);
        totalFrames++;

        if (windowSize < WINDOW_SIZE) {
            return;
        }

        if (probePending && totalFrames - probeStartFrame >= PROBE_FRAMES) {
            finishProbe();
        }

        if (totalFrames - lastEvaluationFrame >= EVALUATE_EVERY_FRAMES) {
            lastEvaluationFrame = totalFrames;
            evaluate();
        }
    }

    private static void evaluate() {
        if (!VgtuConfig.autoScale || likelyCpuOrPresentBound) {
            return;
        }
        if (totalFrames - lastScaleChangeFrame < CHANGE_COOLDOWN_FRAMES) {
            return;
        }

        double p90Ms = percentile90Ms();
        double budgetMs = 1000.0 / VgtuConfig.targetFps;

        if (p90Ms > budgetMs * 1.08) {
            overBudgetWindows++;
            underBudgetWindows = 0;
        } else if (p90Ms < budgetMs * 0.80) {
            underBudgetWindows++;
            overBudgetWindows = 0;
        } else {
            overBudgetWindows = 0;
            underBudgetWindows = 0;
        }

        if (overBudgetWindows >= 3) {
            int next = Math.max(VgtuConfig.minScale,
                    VgtuConfig.renderScale - VgtuConfig.step);
            if (next < VgtuConfig.renderScale) {
                requestScale(next, true, p90Ms);
            }
            overBudgetWindows = 0;
        } else if (underBudgetWindows >= 6) {
            int next = Math.min(VgtuConfig.maxScale,
                    VgtuConfig.renderScale + VgtuConfig.step);
            if (next > VgtuConfig.renderScale) {
                requestScale(next, false, p90Ms);
            }
            underBudgetWindows = 0;
        }
    }

    private static void requestScale(int nextScale, boolean isDownscale, double beforeMs) {
        int previous = VgtuConfig.renderScale;
        VgtuConfig.renderScale = nextScale;
        VitrailBridge.setRenderScale(nextScale);
        VgtuConfig.save();

        lastScaleChangeFrame = totalFrames;
        overBudgetWindows = 0;
        underBudgetWindows = 0;

        if (isDownscale) {
            probePending = true;
            probeStartFrame = totalFrames;
            probeBeforeMs = beforeMs;
        } else {
            probePending = false;
            noGainSteps = 0;
        }

        VgtuClient.LOGGER.info("VGTU adaptive scale: {}% -> {}% (frame-time p90: {} ms)",
                previous, nextScale, Math.round(beforeMs * 100.0) / 100.0);
    }

    private static void finishProbe() {
        double afterMs = percentile90Ms();
        double improvement = probeBeforeMs <= 0.0
                ? 0.0
                : (probeBeforeMs - afterMs) / probeBeforeMs;

        if (improvement < 0.04) {
            noGainSteps++;
        } else {
            noGainSteps = 0;
        }
        probePending = false;

        if (noGainSteps >= 2) {
            likelyCpuOrPresentBound = true;
            int currentScale = VgtuConfig.renderScale;
            VgtuConfig.renderScale = baselineScale;
            VitrailBridge.setRenderScale(baselineScale);
            VgtuConfig.save();
            VgtuClient.LOGGER.warn("VGTU paused adaptive downscaling and restored its starting scale "
                    + "({}% from {}%): lowering resolution did not meaningfully improve frame time. "
                    + "The session may be CPU-bound or limited by VSync/frame pacing. Use /vgtu reset "
                    + "to try again.", baselineScale, currentScale);
        }
    }

    public static synchronized double percentile90Ms() {
        if (windowSize == 0) {
            return 0.0;
        }
        System.arraycopy(FRAME_TIMES, 0, SORT_BUFFER, 0, windowSize);
        Arrays.sort(SORT_BUFFER, 0, windowSize);
        int index = Math.max(0, (int) Math.ceil(windowSize * 0.90) - 1);
        return SORT_BUFFER[index] / 1_000_000.0;
    }

    public static synchronized void setAutoScale(boolean enabled) {
        VgtuConfig.autoScale = enabled;
        if (enabled) {
            baselineScale = VgtuConfig.renderScale;
        }
        VgtuConfig.save();
        resetControllerState();
    }

    public static synchronized void setManualScale(int percent) {
        VgtuConfig.renderScale = Math.max(VgtuConfig.minScale,
                Math.min(VgtuConfig.maxScale, percent));
        baselineScale = VgtuConfig.renderScale;
        VgtuConfig.autoScale = false;
        VitrailBridge.setRenderScale(VgtuConfig.renderScale);
        VgtuConfig.save();
        resetControllerState();
    }

    public static synchronized void setTargetFps(int fps) {
        VgtuConfig.targetFps = Math.max(30, Math.min(240, fps));
        VgtuConfig.save();
        resetControllerState();
    }

    public static synchronized void resetControllerState() {
        baselineScale = VgtuConfig.renderScale;
        overBudgetWindows = 0;
        underBudgetWindows = 0;
        probePending = false;
        noGainSteps = 0;
        likelyCpuOrPresentBound = false;
        lastEvaluationFrame = totalFrames;
        lastScaleChangeFrame = totalFrames;
    }

    public static synchronized boolean isLikelyCpuOrPresentBound() {
        return likelyCpuOrPresentBound;
    }

    public static synchronized double frameTimeP90Ms() {
        return percentile90Ms();
    }
}
