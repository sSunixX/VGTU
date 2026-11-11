package dev.vgtu;

import dev.vgtu.command.VgtuCommands;
import dev.vgtu.config.VgtuConfig;
import dev.vgtu.performance.PerformanceGovernor;
import dev.vgtu.render.VitrailBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VgtuClient implements ClientModInitializer {
    public static final String MOD_ID = "vgtu";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        VgtuConfig.load();
        PerformanceGovernor.initialize();
        VitrailBridge.applyConfig();

        ClientCommandRegistrationCallback.EVENT.register(VgtuCommands::register);

        if (FabricLoader.getInstance().isModLoaded("siris")) {
            LOGGER.warn("SIRIS is installed. Its DLSS path can override Vitrail's render scale; "
                    + "disable DLSS before testing VGTU's adaptive scale.");
        }

        LOGGER.info("VGTU {} initialised for Minecraft 26.3. Adaptive scale: {}, target: {} FPS, "
                        + "starting scale: {}%. Use /vgtu for controls.",
                "0.1.0-alpha.1",
                VgtuConfig.autoScale,
                VgtuConfig.targetFps,
                VgtuConfig.renderScale);
    }
}
