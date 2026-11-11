package dev.vgtu.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class VgtuConfig {
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("vgtu.properties");

    public static boolean autoScale = true;
    public static int targetFps = 60;
    public static int renderScale = 100;
    public static int minScale = 50;
    public static int maxScale = 100;
    public static int step = 3;
    public static boolean temporalAccumulation = false;

    private VgtuConfig() {
    }

    public static void load() {
        Properties properties = new Properties();

        if (Files.isRegularFile(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                properties.load(reader);
            } catch (IOException exception) {
                dev.vgtu.VgtuClient.LOGGER.warn("Could not read {}", FILE, exception);
            }
        }

        autoScale = Boolean.parseBoolean(properties.getProperty("autoScale", "true"));
        targetFps = clamp(parseInt(properties, "targetFps", 60), 30, 240);
        minScale = clamp(parseInt(properties, "minScale", 50), 25, 99);
        maxScale = clamp(parseInt(properties, "maxScale", 100), minScale + 1, 100);
        step = clamp(parseInt(properties, "step", 3), 1, 10);
        renderScale = clamp(parseInt(properties, "renderScale", 100), minScale, maxScale);
        temporalAccumulation =
                Boolean.parseBoolean(properties.getProperty("temporalAccumulation", "false"));

        save();
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("autoScale", Boolean.toString(autoScale));
        properties.setProperty("targetFps", Integer.toString(targetFps));
        properties.setProperty("renderScale", Integer.toString(renderScale));
        properties.setProperty("minScale", Integer.toString(minScale));
        properties.setProperty("maxScale", Integer.toString(maxScale));
        properties.setProperty("step", Integer.toString(step));
        properties.setProperty("temporalAccumulation", Boolean.toString(temporalAccumulation));

        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                properties.store(writer, "VGTU settings - Minecraft 26.3");
            }
        } catch (IOException exception) {
            dev.vgtu.VgtuClient.LOGGER.warn("Could not save {}", FILE, exception);
        }
    }

    private static int parseInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
