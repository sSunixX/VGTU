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
                                    return reply(context.getSource(), "Adaptive FPS target set to " + fps
                                            + " FPS. This is not an FPS cap; VGTU adjusts render scale toward it.");
                                })))
                .then(ClientCommands.literal("scale")
                        .then(ClientCommands.argument("percent", IntegerArgumentType.integer(25, 100))
                                .executes(context -> {
                                    int scale = IntegerArgumentType.getInteger(context, "percent");
                                    PerformanceGovernor.setManualScale(scale);
                                    return reply(context.getSource(), "Manual render scale set to "
                                            + VgtuConfig.renderScale + "%. Adaptive mode is now off.");
                                })))
                .then(ClientCommands.literal("temporal")
                        .then(ClientCommands.literal("on")
                                .executes(context -> {
                                    if (!VitrailBridge.setTemporalAccumulation(true)) {
                                        return reply(context.getSource(),
                                                "Vitrail did not accept the temporal-accumulation setting.");
                                    }
                                    VgtuConfig.temporalAccumulation = true;
                                    VgtuConfig.save();
                                    return reply(context.getSource(), "Vitrail temporal accumulation requested. "
                                            + "It only runs with an active Vitrail shader pack and render scale below 100%. "
                                            + "Its current motion data is camera-derived, so moving entities may ghost.");
                                }))
                        .then(ClientCommands.literal("off")
                                .executes(context -> {
                                    boolean accepted = VitrailBridge.setTemporalAccumulation(false);
                                    VgtuConfig.temporalAccumulation = false;
                                    VgtuConfig.save();
                                    return reply(context.getSource(), accepted
                                            ? "Vitrail temporal accumulation disabled."
                                            : "VGTU saved temporal accumulation as disabled; Vitrail could not confirm the live change.");
                                })))
                .then(ClientCommands.literal("reset")
                        .executes(context -> {
                            PerformanceGovernor.resetControllerState();
                            return reply(context.getSource(),
                                    "Adaptive controller reset. It will sample a fresh frame-time window.");
                        })));
    }

    private static int status(FabricClientCommandSource source) {
        boolean packActive = VitrailBridge.isShaderPackActive();
        boolean temporalWanted = VitrailBridge.isTemporalAccumulationWanted();
        String pack = packActive
                ? "Vitrail pack: active"
                : "Vitrail pack: inactive";
        String temporal = "Temporal accumulation: " + (temporalWanted ? "requested" : "off")
                + (VgtuConfig.renderScale >= 100 ? " (inactive at 100% scale)"
                : packActive ? " (eligible to run; requires valid motion vectors)"
                : " (waiting for active Vitrail pack)");
        String limitation = PerformanceGovernor.isLikelyCpuOrPresentBound()
                ? " | scaling paused: likely CPU/VSync-bound; restored starting scale"
                : "";
        String message = "VGTU | Auto: " + VgtuConfig.autoScale
                + " | Adaptive FPS target: " + VgtuConfig.targetFps + " (not a frame-rate cap)"
                + " | Scale: " + VgtuConfig.renderScale + "%"
                + " | Frame-time p90: "
                + Math.round(PerformanceGovernor.frameTimeP90Ms() * 100.0) / 100.0 + " ms"
                + " | " + temporal + " | " + pack + limitation;
        return reply(source, message);
    }

    private static int reply(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal(message));
        return 1;
    }
}
