package dev.vgtu.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.vgtu.config.VgtuConfig;
import dev.vgtu.performance.PerformanceGovernor;
import dev.vgtu.render.VitrailBridge;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

public final class VgtuCommands {
    private VgtuCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                CommandBuildContext commandBuildContext) {
        dispatcher.register(ClientCommands.literal("vgtu")
                .executes(context -> status(context.getSource()))
                .then(ClientCommands.literal("auto")
                        .then(ClientCommands.literal("on")
                                .executes(context -> {
                                    PerformanceGovernor.setAutoScale(true);
                                    return reply(context.getSource(), "Adaptive render scale enabled.");
                                }))
                        .then(ClientCommands.literal("off")
                                .executes(context -> {
                                    PerformanceGovernor.setAutoScale(false);
                                    return reply(context.getSource(), "Adaptive render scale disabled.");
                                })))
                .then(ClientCommands.literal("target")
                        .then(ClientCommands.argument("fps", IntegerArgumentType.integer(30, 240))
                                .executes(context -> {
                                    int fps = IntegerArgumentType.getInteger(context, "fps");
                                    PerformanceGovernor.setTargetFps(fps);
                                    return reply(context.getSource(),
                                            "Target frame rate set to " + fps + " FPS.");
                                })))
                .then(ClientCommands.literal("scale")
                        .then(ClientCommands.argument("percent", IntegerArgumentType.integer(25, 100))
                                .executes(context -> {
                                    int scale = IntegerArgumentType.getInteger(context, "percent");
                                    PerformanceGovernor.setManualScale(scale);
                                    return reply(context.getSource(),
                                            "Manual render scale set to "
                                                    + VgtuConfig.renderScale
                                                    + "%. Auto mode is now off.");
                                })))
                .then(ClientCommands.literal("temporal")
                        .then(ClientCommands.literal("on")
                                .executes(context -> {
                                    if (!VitrailBridge.setTemporalAccumulation(true)) {
                                        return reply(context.getSource(),
                                                "This Vitrail build does not expose the optional temporal hook.");
                                    }
                                    VgtuConfig.temporalAccumulation = true;
                                    VgtuConfig.save();
                                    return reply(context.getSource(),
                                            "Vitrail temporal accumulation enabled. Watch for ghosting on moving entities.");
                                }))
                        .then(ClientCommands.literal("off")
                                .executes(context -> {
                                    VitrailBridge.setTemporalAccumulation(false);
                                    VgtuConfig.temporalAccumulation = false;
                                    VgtuConfig.save();
                                    return reply(context.getSource(),
                                            "Vitrail temporal accumulation disabled.");
                                })))
                .then(ClientCommands.literal("reset")
                        .executes(context -> {
                            PerformanceGovernor.resetControllerState();
                            return reply(context.getSource(),
                                    "Adaptive controller reset. It will sample a fresh frame-time window.");
                        })));
    }

    private static int status(FabricClientCommandSource source) {
        String limitation = PerformanceGovernor.isLikelyCpuOrPresentBound()
                ? " | scaling paused: likely CPU/VSync-bound; restored starting scale"
                : "";
        String active = VitrailBridge.isShaderPackActive()
                ? " | Vitrail shader pack active"
                : " | waiting for an active Vitrail shader pack";
        String message = "VGTU | Auto: " + VgtuConfig.autoScale
                + " | Target: " + VgtuConfig.targetFps + " FPS"
                + " | Scale: " + VgtuConfig.renderScale + "%"
                + " | Frame-time p90: "
                + Math.round(PerformanceGovernor.frameTimeP90Ms() * 100.0) / 100.0 + " ms"
                + " | Temporal: " + VgtuConfig.temporalAccumulation
                + active + limitation;
        return reply(source, message);
    }

    private static int reply(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal(message));
        return 1;
    }
}
