package me.cubix;

import me.cubix.core.Window;
import me.cubix.gfx.Renderer3D;
import me.cubix.ui.*;

import static org.lwjgl.glfw.GLFW.*;

public final class Game {

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
                // TODO: 월드 생성
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
        double last = window.time();
        while (!window.shouldClose()) {
            double now = window.time();
            float dt = (float)(now - last);
            last = now;

            ui.beginInput();

            window.pollEvents();

            ui.endInput();

            if (state == state.MENU) {
                if (glfwGetKey(window.handle(), GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                    menu.menuBack();
                }
            }

            if (state == State.PLAY) {
                // TODO: 플레이 구현
                if (glfwGetKey(window.handle(), GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                    state = State.MENU;
                    glfwSetInputMode(window.handle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                }
            }

            renderer3D.render(dt);

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