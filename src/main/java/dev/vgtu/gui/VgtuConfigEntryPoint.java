package dev.vgtu.gui;

import dev.vgtu.config.VgtuConfig;
import dev.vgtu.performance.PerformanceGovernor;
import dev.vgtu.render.VitrailBridge;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Registers VGTU controls in Sodium's Video Settings screen. */
public final class VgtuConfigEntryPoint implements ConfigEntryPoint {
    private static final StorageEventHandler STORAGE_HANDLER = VgtuConfig::save;

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        ModOptionsBuilder options = builder.registerOwnModOptions();

        options.addPage(builder.createOptionPage()
                .setName(Component.literal("Upscaling"))
                .addOptionGroup(builder.createOptionGroup()
                        .setName(Component.literal("Render quality"))
                        .addOption(builder.createEnumOption(
                                        Identifier.parse("vgtu:quality_preset"), QualityPreset.class)
                                .setName(Component.literal("Quality Preset"))
                                .setTooltip(Component.literal(
                                        "Choose a starting render scale. Presets use Vitrail's existing Vulkan reconstruction path."))
                                .setElementNameProvider(value -> Component.literal(value.label))
                                .setDefaultValue(QualityPreset.NATIVE)
                                .setImpact(OptionImpact.VARIES)
                                .setBinding(QualityPreset::apply,
                                        () -> QualityPreset.fromScale(VgtuConfig.renderScale))
                                .setStorageHandler(STORAGE_HANDLER))
                        .addOption(builder.createIntegerOption(Identifier.parse("vgtu:render_scale"))
                                .setName(Component.literal("Render Scale"))
                                .setTooltip(Component.literal(
                                        "World render resolution as a percentage of the window. The HUD and menus remain at native resolution."))
                                .setRange(25, 100, 1)
                                .setValueFormatter(value -> Component.literal(value + "%"))
                                .setDefaultValue(100)
                                .setImpact(OptionImpact.VARIES)
                                .setBinding(PerformanceGovernor::setManualScale,
                                        () -> VgtuConfig.renderScale)
                                .setStorageHandler(STORAGE_HANDLER))))
                .addPage(builder.createOptionPage()
                        .setName(Component.literal("Adaptive Scaling"))
                        .addOptionGroup(builder.createOptionGroup()
                                .setName(Component.literal("Frame-rate target"))
                                .addOption(builder.createBooleanOption(Identifier.parse("vgtu:auto_scale"))
                                        .setName(Component.literal("Adaptive Render Scale"))
                                        .setTooltip(Component.literal(
                                                "Adjusts render scale gradually to approach the target below. It cannot improve CPU-bound or VSync-bound scenes."))
                                        .setDefaultValue(true)
                                        .setBinding(PerformanceGovernor::setAutoScale,
                                                () -> VgtuConfig.autoScale)
                                        .setStorageHandler(STORAGE_HANDLER))
                                .addOption(builder.createIntegerOption(Identifier.parse("vgtu:target_fps"))
                                        .setName(Component.literal("Adaptive FPS Target"))
                                        .setTooltip(Component.literal(
                                                "The adaptive scaler's goal, not an FPS limiter or frame-rate cap."))
                                        .setRange(30, 240, 5)
                                        .setValueFormatter(value -> Component.literal(value + " FPS"))
                                        .setDefaultValue(60)
                                        .setBinding(PerformanceGovernor::setTargetFps,
                                                () -> VgtuConfig.targetFps)
                                        .setStorageHandler(STORAGE_HANDLER))
                                .addOption(builder.createBooleanOption(Identifier.parse("vgtu:temporal_accumulation"))
                                        .setName(Component.literal("Temporal Accumulation"))
                                        .setTooltip(Component.literal(
                                                "Uses Vitrail's optional history-fold pass. It only runs with an active Vitrail pack and render scale below 100%; moving entities may ghost."))
                                        .setDefaultValue(false)
                                        .setBinding(VgtuConfigEntryPoint::setTemporal,
                                                VitrailBridge::isTemporalAccumulationWanted)
                                        .setStorageHandler(STORAGE_HANDLER)))
                        .addOptionGroup(builder.createOptionGroup()
                                .setName(Component.literal("Adaptive response"))
                                .addOption(builder.createIntegerOption(Identifier.parse("vgtu:scale_step"))
                                        .setName(Component.literal("Scale Adjustment Step"))
                                        .setTooltip(Component.literal(
                                                "How many percentage points render scale changes per adjustment."))
                                        .setRange(1, 10, 1)
                                        .setValueFormatter(value -> Component.literal(value + "%"))
                                        .setDefaultValue(3)
                                        .setBinding(VgtuConfigEntryPoint::setStep,
                                                () -> VgtuConfig.step)
                                        .setStorageHandler(STORAGE_HANDLER))));
    }

    private static void setTemporal(boolean enabled) {
        if (VitrailBridge.setTemporalAccumulation(enabled)) {
            VgtuConfig.temporalAccumulation = enabled;
            VgtuConfig.save();
        }
    }

    private static void setStep(int step) {
        VgtuConfig.step = Math.max(1, Math.min(10, step));
        VgtuConfig.save();
        PerformanceGovernor.resetControllerState();
    }

    private enum QualityPreset {
        NATIVE(100, "Native (100%)"),
        ULTRA_QUALITY(85, "Ultra Quality (85%)"),
        QUALITY(75, "Quality (75%)"),
        BALANCED(67, "Balanced (67%)"),
        PERFORMANCE(50, "Performance (50%)"),
        CUSTOM(-1, "Custom");

        private final int scale;
        private final String label;

        QualityPreset(int scale, String label) {
            this.scale = scale;
            this.label = label;
        }

        private void apply() {
            if (this.scale >= 0) {
                PerformanceGovernor.setManualScale(this.scale);
            }
        }

        private static QualityPreset fromScale(int scale) {
            for (QualityPreset preset : values()) {
                if (preset.scale == scale) {
                    return preset;
                }
            }
            return CUSTOM;
        }
    }
}
