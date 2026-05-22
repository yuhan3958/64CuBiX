package me.cubix;

import me.cubix.core.Window;
import me.cubix.gameplay.Player;
import me.cubix.gfx.Renderer3D;
import me.cubix.ui.GameMenu;
import me.cubix.ui.I18n;
import me.cubix.ui.MenuActions;
import me.cubix.ui.MenuState;
import me.cubix.ui.NuklearGL3;
import me.cubix.world.World;
import me.cubix.world.WorldInfo;
import org.joml.Vector3f;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;

import static me.cubix.world.save.WorldStorage.saveDirtyChunks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nuklear.Nuklear.*;

public final class Game {

    private boolean jumpDown = false;
    private boolean escapeDown = false;

    private World world; // 현재 플레이 중인 월드. 메뉴 상태면 null

    public World world() { return world; }

    public void setWorld(World w) { this.world = w; }

    private enum State { MENU, PLAY, INGAME_MENU }

    private Window window;
    private Renderer3D renderer3D;

    private State state = State.MENU;

    // UI
    private NuklearGL3 ui;
    private final MenuState menuState = new MenuState();
    private GameMenu menu;

    private final Vector3f lastCam = new Vector3f();
    private final Vector3f desiredDelta = new Vector3f();

    private final Player player = new Player();

    public void run() {
        window = new Window(1280, 720, "64cubix");
        window.init();

        renderer3D = new Renderer3D(window);
        renderer3D.init();

        ui = new NuklearGL3(window.handle());
        ui.init();

        menu = new GameMenu(menuState, new MenuActions() {
            @Override public void startSingleplayer(WorldInfo world) {
                System.out.println("[MENU] Start world: " + world.name() + " seed=" + world.seed());
                World w = new World(world.seed(), world);
                w.getBlock(0, 0, 0);
                spawnPlayerAtTopBlock(w, 0, 0);
                setWorld(w);
                state = State.PLAY;
                glfwSetInputMode(window.handle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }
            @Override public void backToMenu() {
                state = State.MENU;
                glfwSetInputMode(window.handle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            }
            @Override public void quit() {
                window.setShouldClose(true);
            }
        });


        glfwSetInputMode(window.handle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        loop();

        ui.cleanup();
        renderer3D.cleanup();
        window.cleanup();
    }

    private void loop() {
        while (!window.shouldClose()) {
            float dt=1/64f;

            float playerHeight=menu.getState().playerHeight/100f;

            ui.beginInput();

            window.pollEvents();

            ui.endInput();

            if (state == State.MENU) {
                if (consumeEscapePress()) {
                    menu.menuBack();
                }
            }

            if (state == State.PLAY) {

                if (consumeEscapePress()) {
                    state = State.INGAME_MENU;
                    glfwSetInputMode(window.handle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                }
            }

            renderer3D.render(dt, world, state == State.PLAY, state == State.PLAY);

            desiredDelta.set(renderer3D.camera.position).sub(lastCam); // 카메라가 가고 싶었던 이동량

            desiredDelta.y=0f;
            if (state != State.PLAY) {
                desiredDelta.zero();
            }

            if (state == State.PLAY) {
                boolean inWater = player.isInWater(world, playerHeight);
                if (inWater) {
                    desiredDelta.mul(player.waterMoveMultiplier);
                }

                if (player.onGround && !inWater) {
                    player.airborneTime = 0f;
                } else {
                    player.airborneTime += dt;
                }

                float gravityAccel = Math.min(player.maxGravity, player.gravity + player.gravityGrowth * player.airborneTime);
                if (inWater) {
                    gravityAccel *= player.waterGravityMultiplier;
                }
                player.vel.y -= gravityAccel * dt;

                if (inWater && glfwGetKey(window.handle(), GLFW_KEY_SPACE) == GLFW_PRESS) {
                    player.vel.y = player.waterRiseSpeed;
                }

                if (!inWater && player.onGround && consumeJumpPress()) {
                    player.vel.y = player.jumpSpeed;
                    player.onGround = false;
                }

                Vector3f delta = new Vector3f(
                        desiredDelta.x,
                        player.vel.y * dt,
                        desiredDelta.z
                );

                player.moveAndCollide(world, delta, playerHeight);

                if (player.onGround && player.vel.y < 0f) {
                    player.vel.y = 0f;
                }

                renderer3D.camera.position.set(player.pos.x,
                        player.pos.y + playerHeight,
                        player.pos.z);

                lastCam.set(renderer3D.camera.position);
            } else {
                lastCam.set(renderer3D.camera.position);
            }

            // UI frame
            ui.beginDraw(window.width(), window.height());

            if (state == State.MENU) {
                menu.draw(ui.ctx(), window.width(), window.height());
            } else if (state == State.INGAME_MENU) {
                drawInGameMenu(ui.ctx(), window.width(), window.height());
            }
            ui.endDraw();

            if (state == State.PLAY) {
                renderer3D.renderCrosshair(window.width(), window.height());
            }

            window.swapBuffers();
        }
    }

    private void spawnPlayerAtTopBlock(World world, int x, int z) {
        float y = world.topSolidSpawnY(x, z);
        player.pos.set(x + 0.5f, y, z + 0.5f);
        player.vel.zero();
        player.onGround = false;
        player.airborneTime = 0f;
        renderer3D.camera.position.set(player.pos.x, player.pos.y, player.pos.z);
        lastCam.set(renderer3D.camera.position);
    }

    private boolean consumeEscapePress() {
        boolean down = glfwGetKey(window.handle(), GLFW_KEY_ESCAPE) == GLFW_PRESS;
        if (down && !escapeDown) {
            escapeDown = true;
            return true;
        }
        if (!down) {
            escapeDown = false;
        }
        return false;
    }

    private boolean consumeJumpPress() {
        boolean down = glfwGetKey(window.handle(), GLFW_KEY_SPACE) == GLFW_PRESS;
        if (down && !jumpDown) {
            jumpDown = true;
            return true;
        }
        if (!down) {
            jumpDown = false;
        }
        return false;
    }

    private void drawInGameMenu(NkContext ctx, int w, int h) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NkRect rect = NkRect.malloc(stack);
            float ww = Math.min(360f, w * 0.36f);
            float hh = 190f;
            float x = (w - ww) * 0.5f;
            float y = (h - hh) * 0.45f;

            nk_begin(ctx, t("ingame.title"),
                    nk_rect(x, y, ww, hh, rect),
                    NK_WINDOW_BORDER | NK_WINDOW_TITLE | NK_WINDOW_NO_SCROLLBAR);

            nk_layout_row_dynamic(ctx, 36, 1);
            if (nk_button_label(ctx, t("ingame.resume"))) {
                state = State.PLAY;
                renderer3D.camera.resetMouse();
                glfwSetInputMode(window.handle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }

            nk_layout_row_dynamic(ctx, 36, 1);
            if (nk_button_label(ctx, t("ingame.quit_to_main"))) {
                quitToMainMenu();
            }

            nk_end(ctx);
        }
    }

    private void quitToMainMenu() {
        World w = world();
        if (w != null) {
            try {
                saveDirtyChunks(w);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("[SAVE] quitToMenu called");
        System.out.println("[SAVE] world=" + (world() == null ? "null" : world().info().name()));
        state = State.MENU;
        setWorld(null);
        glfwSetInputMode(window.handle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    private String t(String key) {
        return I18n.t(menuState.language, key);
    }
}
