package me.cubix.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SettingsStorage {
    private final Path file;

    public SettingsStorage() {
        this(Path.of("setting.ini"));
    }

    public SettingsStorage(Path file) {
        this.file = file;
    }

    public void loadInto(MenuState state) {
        if (!Files.exists(file)) return;

        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        state.showCelsius = parseBoolean(p.getProperty("showCelsius"), state.showCelsius);
        state.mouseSensitivity = clamp(parseFloat(p.getProperty("mouseSensitivity"), state.mouseSensitivity), 0.02f, 0.40f);
        state.playerHeight = clamp(parseInt(p.getProperty("playerHeight"), state.playerHeight), 100, 200);
        state.language = parseLanguage(p.getProperty("language"), state.language);
    }

    public void save(MenuState state) {
        Properties p = new Properties();
        p.setProperty("showCelsius", Boolean.toString(state.showCelsius));
        p.setProperty("mouseSensitivity", Float.toString(state.mouseSensitivity));
        p.setProperty("playerHeight", Integer.toString(state.playerHeight));
        p.setProperty("language", state.language);

        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "64cubix settings");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if (value == null) return fallback;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        return fallback;
    }

    private static float parseFloat(String value, float fallback) {
        if (value == null) return fallback;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String parseLanguage(String value, String fallback) {
        if (I18n.KO_KR.equals(value) || I18n.EN_US.equals(value)) return value;
        return fallback;
    }
}
