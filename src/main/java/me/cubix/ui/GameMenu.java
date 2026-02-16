package me.cubix.ui;

import me.cubix.world.World;
import org.lwjgl.BufferUtils;
import org.lwjgl.nuklear.*;
import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import me.cubix.world.block.BlockId;

import java.nio.IntBuffer;

import static org.lwjgl.nuklear.Nuklear.*;

public final class GameMenu {
    private final MenuState s;
    private final MenuActions actions;
    private final WorldInfoStorage storage = new WorldInfoStorage();

    private final ByteBuffer nameBuf = BufferUtils.createByteBuffer(64);
    private final IntBuffer nameLen = BufferUtils.createIntBuffer(1);

    private final ByteBuffer seedBuf = BufferUtils.createByteBuffer(64);
    private final IntBuffer seedLen = BufferUtils.createIntBuffer(1);

    public final static String BACK="돌아가기";

    public GameMenu(MenuState state, MenuActions actions) {
        this.s = state;
        this.actions = actions;

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

            nk_begin(ctx, "메뉴",
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

        if (nk_button_label(ctx, "싱글플레이")) {
            refreshWorlds();
            s.screen = MenuScreen.SINGLEPLAYER;
        }
        if (nk_button_label(ctx, "멀티플레이")) {
            s.screen = MenuScreen.MULTIPLAYER;
        }
        if (nk_button_label(ctx, "설정")) {
            s.screen = MenuScreen.OPTIONS;
        }
        if (nk_button_label(ctx, "종료")) {
            actions.quit();
        }
    }

    private void drawSingle(NkContext ctx) {
        nk_layout_row_dynamic(ctx, 22, 1);
        nk_label(ctx, "세계", NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 240, 1);
        nk_group_begin(ctx, "세계 목록", NK_WINDOW_BORDER);
        {
            nk_layout_row_dynamic(ctx, 22, 1);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                for (int i = 0; i < s.worlds.size(); i++) {
                    WorldInfo wi = s.worlds.get(i);

                    ByteBuffer selected = stack.malloc(1);
                    selected.put(0, (byte) ((i == s.selectedWorld) ? 1 : 0));

                    if (nk_selectable_label(ctx, wi.name(), NK_TEXT_LEFT, selected)) {
                        // nk_selectable_label은 내부에서 selected 값을 토글할 수 있음
                    }
                    if (selected.get(0) != 0) {
                        s.selectedWorld = i;
                    }
                }
            }

        }
        nk_group_end(ctx);

        boolean hasSel = s.selectedWorld >= 0 && s.selectedWorld < s.worlds.size();

        nk_layout_row_dynamic(ctx, 36, 2);
        if (nk_button_label(ctx, "플레이") && hasSel) {
            actions.startSingleplayer(s.worlds.get(s.selectedWorld));
        }
        if (nk_button_label(ctx, "만들기")) {
            s.screen = MenuScreen.CREATE_WORLD;
        }

        nk_layout_row_dynamic(ctx, 36, 2);
        if (nk_button_label(ctx, "삭제") && hasSel) {
            s.pendingDelete = s.selectedWorld;
            s.screen = MenuScreen.CONFIRM_DELETE;
        }
        if (nk_button_label(ctx, BACK)) {
            s.screen = MenuScreen.MAIN;
        }
    }

    private void drawCreate(NkContext ctx) {
        nk_layout_row_dynamic(ctx, 22, 1);
        nk_label(ctx, "세계 이름", NK_TEXT_LEFT);

        ensureNullTerminated(nameBuf, nameLen);
        ensureNullTerminated(seedBuf, seedLen);

        nk_layout_row_dynamic(ctx, 28, 1);
        nk_edit_string(ctx, NK_EDIT_FIELD, nameBuf, nameLen, 63, null);

        nk_layout_row_dynamic(ctx, 22, 1);
        nk_label(ctx, "시드", NK_TEXT_LEFT);

        ensureNullTerminated(nameBuf, nameLen);
        ensureNullTerminated(seedBuf, seedLen);

        nk_layout_row_dynamic(ctx, 28, 1);
        nk_edit_string(ctx, NK_EDIT_FIELD, seedBuf, seedLen, 63, null);

        nk_layout_row_dynamic(ctx, 36, 2);

        if (nk_button_label(ctx, "만들기")) {
            String name = getString(nameBuf, nameLen).trim();
            String seedText = getString(seedBuf, seedLen).trim();
            if (name.isEmpty()) name = "이름 없는 세계";

            long seed = parseSeed(seedText);

            try {
                WorldInfo info = storage.createWorld(name, seed);
                refreshWorlds();
                World world = new World(seed, info);
                for (int z = -16; z <= 16; z++) {
                    StringBuilder sb = new StringBuilder();
                    for (int x = -32; x <= 32; x++) {
                        int y = 80;
                        while (y > 0 && world.getBlock(x, y, z) == BlockId.AIR) y--;
                        sb.append(y < 10 ? " ." : y < 20 ? " -" : y < 30 ? " +" : " #");
                    }
                    System.out.println(sb);
                }
                s.screen = MenuScreen.SINGLEPLAYER;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (nk_button_label(ctx, "취소")) {
            s.screen = MenuScreen.SINGLEPLAYER;
        }
    }

    private void drawDeleteConfirm(NkContext ctx) {
        String target = "(none)";
        if (s.pendingDelete >= 0 && s.pendingDelete < s.worlds.size()) {
            target = s.worlds.get(s.pendingDelete).name();
        }

        nk_layout_row_dynamic(ctx, 24, 1);
        nk_label(ctx, "월드를 삭제하시겠습니까?", NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 24, 1);
        nk_label(ctx, target, NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 36, 2);
        if (nk_button_label(ctx, "예")) {
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
        if (nk_button_label(ctx, "아니요")) {
            s.pendingDelete = -1;
            s.screen = MenuScreen.SINGLEPLAYER;
        }
    }

    private void drawOptions(NkContext ctx) {
        nk_layout_row_dynamic(ctx, 24, 1);
        s.showCelsius = nk_check_label(ctx, "온도를 섭씨로 보기", s.showCelsius);

        nk_layout_row_dynamic(ctx, 24, 1);
        nk_label(ctx, "마우스 민감도", NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 24, 1);
        s.mouseSensitivity = nk_slide_float(ctx, 0.02f, s.mouseSensitivity, 0.40f, 0.01f);

        nk_layout_row_dynamic(ctx, 36, 1);
        if (nk_button_label(ctx, BACK)) {
            s.screen = MenuScreen.MAIN;
        }
    }

    private void drawMulti(NkContext ctx) {
        nk_layout_row_dynamic(ctx, 24, 1);
        nk_label(ctx, "(Multiplayer menu placeholder)", NK_TEXT_LEFT);

        nk_layout_row_dynamic(ctx, 36, 1);
        if (nk_button_label(ctx, BACK)) {
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

    private static String title(MenuScreen sc) {
        return switch (sc) {
            case MAIN -> "메인 메뉴";
            case SINGLEPLAYER -> "싱글플레이";
            case CREATE_WORLD -> "세계 만들기";
            case CONFIRM_DELETE -> "삭제 확인";
            case MULTIPLAYER -> "멀티플레이";
            case OPTIONS -> "설정";
        };
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

    private static void setBuf(byte[] buf, String s) {
        for (int i = 0; i < buf.length; i++) buf[i] = 0;
        byte[] b = s.getBytes();
        System.arraycopy(b, 0, buf, 0, Math.min(b.length, buf.length));
    }

    private static void putString(ByteBuffer buf, IntBuffer len, String s) {
        buf.clear();
        byte[] bytes = s.getBytes();
        int n = Math.min(bytes.length, buf.capacity() - 1);
        buf.put(0, bytes, 0, n);
        buf.put(n, (byte) 0);
        len.put(0, n);
    }

    private static String getString(ByteBuffer buf, IntBuffer len) {
        int n = len.get(0);
        if (n < 0) n = 0;
        if (n > buf.capacity() - 1) n = buf.capacity() - 1;

        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) out[i] = buf.get(i);
        return new String(out);
    }

    private static void ensureNullTerminated(ByteBuffer buf, IntBuffer len) {
        int n = len.get(0);
        if (n < 0) n = 0;
        int cap = buf.capacity();

        if (n > cap - 1) {
            n = cap - 1;
            len.put(0, n);
        }

        buf.put(n, (byte) 0);
    }

    public Void menuBack() {
        s.screen = MenuScreen.MAIN;
        return null;
    }
}
