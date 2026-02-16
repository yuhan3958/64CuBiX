package me.cubix;

import me.cubix.core.Window;
import me.cubix.gfx.Renderer3D;
import me.cubix.ui.*;
import me.cubix.world.World;
import me.cubix.world.save.WorldStorage;

import java.io.IOException;

import static me.cubix.world.save.WorldStorage.saveDirtyChunks;
import static org.lwjgl.glfw.GLFW.*;

public final class Game {

    private World world; // 현재 플레이 중인 월드. 메뉴 상태면 null

    public World world() { return world; }

    public void setWorld(World w) { this.world = w; }

    private enum State { MENU, PLAY }

    private Window window;
    private Renderer3D renderer3D;

    private State state = State.MENU;

    // UI
    private NuklearGL3 ui;
    private final MenuState menuState = new MenuState();
    private GameMenu menu;

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
                state = State.PLAY;
                setWorld(w);
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
        double last = window.time();
        while (!window.shouldClose()) {
            double now = window.time();
            float dt = (float)(now - last);
            last = now;

            ui.beginInput();

            window.pollEvents();

            ui.endInput();

            if (state == State.MENU) {
                if (glfwGetKey(window.handle(), GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                    menu.menuBack();
                }
            }

            if (state == State.PLAY) {
                // TODO: 플레이 구현
                if (glfwGetKey(window.handle(), GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                    World w = world();
                    if (w != null) {
                        try {
                            WorldStorage.saveDirtyChunks(w);
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
            }

            renderer3D.render(dt, world());

            // UI frame
            ui.beginDraw(window.width(), window.height());

            if (state == State.MENU) {
                menu.draw(ui.ctx(), window.width(), window.height());
            } else {

            }
            ui.endDraw();

            window.swapBuffers();
        }
    }
}