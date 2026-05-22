package me.cubix.ui;

import me.cubix.world.WorldInfo;
import me.cubix.world.WorldInfoStorage;
import org.lwjgl.BufferUtils;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.nuklear.Nuklear.*;

public final class GameMenu {
    private final MenuState s;
    private final MenuActions actions;
    private final WorldInfoStorage storage = new WorldInfoStorage();
    private final SettingsStorage settingsStorage = new SettingsStorage();

    private final ByteBuffer nameBuf = BufferUtils.createByteBuffer(64);
    private final IntBuffer nameLen = BufferUtils.createIntBuffer(1);

    private final ByteBuffer seedBuf = BufferUtils.createByteBuffer(64);
    private final IntBuffer seedLen = BufferUtils.createIntBuffer(1);

    public GameMenu(MenuState state, MenuActions actions) {
        this.s = state;
        this.actions = actions;
        settingsStorage.loadInto(s);

        if (s.newWorldName == null || s.newWorldName.isBlank()) {
            s.newWorldName = t("world.default_name");
        }

        putString(nameBuf, nameLen, s.newWorldName);
        putString(seedBuf, seedLen, s.newWorldSeed);
    }

    public void draw(NkContext ctx, int w, int h) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NkRect rect = NkRect.malloc(stack);

            float ww = w * 0.42f;
            float hh = h * 0.62f;
            float x = (w - ww) * 0.5f;
            float y = (h - hh) * 0.45f;

            nk_begin(ctx, t("menu.window"),
                    nk_rect(x, y, ww, hh, rect),
                    NK_WINDOW_BORDER | NK_WINDOW_TITLE | NK_WINDOW_NO_SCROLLBAR);

            nk_layout_row_dynamic(ctx, 28, 1);
            nk_label(ctx, title(s.screen), NK_TEXT_CENTERED);

            nk_layout_row_dynamic(ctx, 10, 1);
            nk_spacing(ctx, 1);

            switch (s.screen) {
                case MAIN -> drawMain(ctx);
                case SINGLEPLAYER -> drawSingle(ctx);
                case CREATE_WORLD -> drawCreate(ctx);
                case CONFIRM_DELETE -> drawDeleteConfirm(ctx);
                case OPTIONS -> drawOptions(ctx);
                case MULTIPLAYER -> drawMulti(ctx);
            }

            nk_end(ctx);
        }
    }

    private void drawMain(NkContext ctx) {
        nk_layout_row_dynamic(ctx, 36, 1);

        if (nk_button_label(ctx, t("menu.singleplayer"))) {
            refreshWorlds();
            s.screen = MenuScreen.SINGLEPLAYER;
        }
        if (nk_button_label(ctx, t("menu.multiplayer"))) {
            s.screen = MenuScreen.MULTIPLAYER;
        }
        if (nk_button_label(ctx, t("menu.options"))) {
            s.screen = MenuScreen.OPTIONS;
        }
        if (nk_button_label(ctx, t("menu.quit"))) {
            actions.quit();
        }
    }

    private void drawSingle(NkContext ctx) {
        nk_layout_row_dynamic(ctx, 22, 1);
        nk_label(ctx, t("menu.world"), NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 240, 1);
        nk_group_begin(ctx, t("menu.world_list"), NK_WINDOW_BORDER);
        {
            nk_layout_row_dynamic(ctx, 22, 1);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                for (int i = 0; i < s.worlds.size(); i++) {
                    WorldInfo wi = s.worlds.get(i);

                    ByteBuffer selected = stack.malloc(1);
                    selected.put(0, (byte)((i == s.selectedWorld) ? 1 : 0));

                    nk_selectable_label(ctx, wi.name(), NK_TEXT_LEFT, selected);
                    if (selected.get(0) != 0) {
                        s.selectedWorld = i;
                    }
                }
            }
        }
        nk_group_end(ctx);

        boolean hasSel = s.selectedWorld >= 0 && s.selectedWorld < s.worlds.size();

        nk_layout_row_dynamic(ctx, 36, 2);
        if (nk_button_label(ctx, t("menu.play")) && hasSel) {
            actions.startSingleplayer(s.worlds.get(s.selectedWorld));
        }
        if (nk_button_label(ctx, t("menu.create"))) {
            s.screen = MenuScreen.CREATE_WORLD;
        }

        nk_layout_row_dynamic(ctx, 36, 2);
        if (nk_button_label(ctx, t("menu.delete")) && hasSel) {
            s.pendingDelete = s.selectedWorld;
            s.screen = MenuScreen.CONFIRM_DELETE;
        }
        if (nk_button_label(ctx, t("menu.back"))) {
            s.screen = MenuScreen.MAIN;
        }
    }

    private void drawCreate(NkContext ctx) {
        nk_layout_row_dynamic(ctx, 22, 1);
        nk_label(ctx, t("menu.world_name"), NK_TEXT_LEFT);

        ensureNullTerminated(nameBuf, nameLen);
        ensureNullTerminated(seedBuf, seedLen);

        nk_layout_row_dynamic(ctx, 28, 1);
        nk_edit_string(ctx, NK_EDIT_FIELD, nameBuf, nameLen, 63, null);

        nk_layout_row_dynamic(ctx, 22, 1);
        nk_label(ctx, t("menu.seed"), NK_TEXT_LEFT);

        ensureNullTerminated(nameBuf, nameLen);
        ensureNullTerminated(seedBuf, seedLen);

        nk_layout_row_dynamic(ctx, 28, 1);
        nk_edit_string(ctx, NK_EDIT_FIELD, seedBuf, seedLen, 63, null);

        nk_layout_row_dynamic(ctx, 36, 2);

        if (nk_button_label(ctx, t("menu.create"))) {
            String name = getString(nameBuf, nameLen).trim();
            String seedText = getString(seedBuf, seedLen).trim();
            if (name.isEmpty()) name = t("world.unnamed");

            long seed = parseSeed(seedText);

            try {
                storage.createWorld(name, seed);
                refreshWorlds();
                s.screen = MenuScreen.SINGLEPLAYER;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (nk_button_label(ctx, t("menu.cancel"))) {
            s.screen = MenuScreen.SINGLEPLAYER;
        }
    }

    private void drawDeleteConfirm(NkContext ctx) {
        String target = "(none)";
        if (s.pendingDelete >= 0 && s.pendingDelete < s.worlds.size()) {
            target = s.worlds.get(s.pendingDelete).name();
        }

        nk_layout_row_dynamic(ctx, 24, 1);
        nk_label(ctx, t("menu.delete_confirm_question"), NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 24, 1);
        nk_label(ctx, target, NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 36, 2);
        if (nk_button_label(ctx, t("menu.yes"))) {
            try {
                if (s.pendingDelete >= 0 && s.pendingDelete < s.worlds.size()) {
                    storage.deleteWorld(s.worlds.get(s.pendingDelete));
                }
                s.pendingDelete = -1;
                refreshWorlds();
                s.screen = MenuScreen.SINGLEPLAYER;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (nk_button_label(ctx, t("menu.no"))) {
            s.pendingDelete = -1;
            s.screen = MenuScreen.SINGLEPLAYER;
        }
    }

    private void drawOptions(NkContext ctx) {
        boolean oldShowCelsius = s.showCelsius;
        float oldMouseSensitivity = s.mouseSensitivity;
        int oldPlayerHeight = s.playerHeight;
        String oldLanguage = s.language;

        nk_layout_row_dynamic(ctx, 24, 1);
        s.showCelsius = nk_check_label(ctx, t("options.show_celsius"), s.showCelsius);

        nk_layout_row_dynamic(ctx, 24, 1);
        nk_label(ctx, t("options.mouse_sensitivity"), NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 24, 1);
        s.mouseSensitivity = nk_slide_float(ctx, 0.02f, s.mouseSensitivity, 0.40f, 0.01f);

        nk_layout_row_dynamic(ctx, 24, 1);
        nk_label(ctx, t("options.player_height") + ": " + s.playerHeight, NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 24, 1);
        s.playerHeight = nk_slide_int(ctx, 100, s.playerHeight, 200, 5);

        nk_layout_row_dynamic(ctx, 24, 1);
        nk_label(ctx, t("options.language"), NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 30, 2);
        if (nk_button_label(ctx, languageButtonLabel(I18n.KO_KR, "options.language_ko"))) {
            s.language = I18n.KO_KR;
        }
        if (nk_button_label(ctx, languageButtonLabel(I18n.EN_US, "options.language_en"))) {
            s.language = I18n.EN_US;
        }

        if (oldShowCelsius != s.showCelsius
                || Float.compare(oldMouseSensitivity, s.mouseSensitivity) != 0
                || oldPlayerHeight != s.playerHeight
                || !oldLanguage.equals(s.language)) {
            settingsStorage.save(s);
        }

        nk_layout_row_dynamic(ctx, 36, 1);
        if (nk_button_label(ctx, t("menu.back"))) {
            s.screen = MenuScreen.MAIN;
        }
    }

    private void drawMulti(NkContext ctx) {
        nk_layout_row_dynamic(ctx, 24, 1);
        nk_label(ctx, t("menu.multiplayer_placeholder"), NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 36, 1);
        if (nk_button_label(ctx, t("menu.back"))) {
            s.screen = MenuScreen.MAIN;
        }
    }

    private void refreshWorlds() {
        try {
            storage.ensure();
            s.worlds.clear();
            s.worlds.addAll(storage.listWorlds());
            if (s.worlds.isEmpty()) s.selectedWorld = -1;
            else if (s.selectedWorld < 0) s.selectedWorld = 0;
            else if (s.selectedWorld >= s.worlds.size()) s.selectedWorld = s.worlds.size() - 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String title(MenuScreen sc) {
        return switch (sc) {
            case MAIN -> t("menu.main");
            case SINGLEPLAYER -> t("menu.singleplayer");
            case CREATE_WORLD -> t("menu.create_world");
            case CONFIRM_DELETE -> t("menu.confirm_delete");
            case MULTIPLAYER -> t("menu.multiplayer");
            case OPTIONS -> t("menu.options");
        };
    }

    private String languageButtonLabel(String language, String labelKey) {
        String prefix = s.language.equals(language) ? "* " : "";
        return prefix + t(labelKey);
    }

    private String t(String key) {
        return I18n.t(s.language, key);
    }

    private static long parseSeed(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException e) { return fnv1a64(s.trim()); }
    }

    private static long fnv1a64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    private static void putString(ByteBuffer buf, IntBuffer len, String s) {
        buf.clear();
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int n = Math.min(bytes.length, buf.capacity() - 1);
        for (int i = 0; i < n; i++) {
            buf.put(i, bytes[i]);
        }
        buf.put(n, (byte)0);
        len.put(0, n);
    }

    private static String getString(ByteBuffer buf, IntBuffer len) {
        int n = len.get(0);
        if (n < 0) n = 0;
        if (n > buf.capacity() - 1) n = buf.capacity() - 1;

        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) out[i] = buf.get(i);
        return new String(out, StandardCharsets.UTF_8);
    }

    private static void ensureNullTerminated(ByteBuffer buf, IntBuffer len) {
        int n = len.get(0);
        if (n < 0) n = 0;
        int cap = buf.capacity();

        if (n > cap - 1) {
            n = cap - 1;
            len.put(0, n);
        }

        buf.put(n, (byte)0);
    }

    public Void menuBack() {
        s.screen = MenuScreen.MAIN;
        return null;
    }

    public MenuState getState() {
        return s;
    }
}
