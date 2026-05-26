package me.cubix.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class I18n {
    public static final String KO_KR = "ko_kr";
    public static final String EN_US = "en_us";

    private static final Map<String, Properties> CACHE = new HashMap<>();

    public static String t(String language, String key) {
        Properties p = load(language);
        String value = p.getProperty(key);
        if (value != null) return value;

        if (!EN_US.equals(language)) {
            value = load(EN_US).getProperty(key);
            if (value != null) return value;
        }

        return key;
    }

    private static Properties load(String language) {
        return CACHE.computeIfAbsent(language, I18n::loadFile);
    }

    private static Properties loadFile(String language) {
        Properties p = new Properties();
        String path = "/lang/" + language + ".ini";

        try (InputStream in = I18n.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Missing language file: " + path);
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return p;
    }

    private I18n() {}
}
