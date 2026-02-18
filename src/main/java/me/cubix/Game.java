package me.cubix;

import me.cubix.core.Window;
import me.cubix.gameplay.Player;
import me.cubix.gfx.Renderer3D;
import me.cubix.ui.GameMenu;
import me.cubix.ui.MenuActions;
import me.cubix.ui.MenuState;
import me.cubix.ui.NuklearGL3;
import me.cubix.world.World;
import me.cubix.world.WorldInfo;
import org.joml.Vector3f;

import java.io.IOException;

import static me.cubix.world.save.WorldStorage.saveDirtyChunks;
import static org.lwjgl.glfw.GLFW.*;

public final class Game {

    private boolean JUMPDown = false;

    private boolean JUMPFiredOnce = false;

    private double JUMPPressedAt = 0.0;

    private double JUMPLastRepeatAt = 0.0;

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
            float dt=1/60f;

            float playerHeight=menu.getState().playerHeight/100f;

            ui.beginInput();

            window.pollEvents();

            ui.endInput();

            if (state == State.MENU) {
                if (glfwGetKey(window.handle(), GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                    menu.menuBack();
                }
            }

            if (state == State.PLAY) {

                if (glfwGetKey(window.handle(), GLFW_KEY_ESCAPE) == GLFW_PRESS) {
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
            }

            renderer3D.render(dt, world);

            desiredDelta.set(renderer3D.camera.position).sub(lastCam); // 카메라가 가고 싶었던 이동량

            desiredDelta.y=0f;

            player.vel.y -= player.gravity * dt;

            if (player.onGround && hasJump()) {
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

    private boolean hasJump() {
        boolean down = glfwGetKey(window.handle(), GLFW_KEY_SPACE) == GLFW_PRESS;
        double t = glfwGetTime();

        final double initialDelay = 0.35;
        final double repeatInterval = 0.3;

        if (down) {
            if (!JUMPDown) {
                JUMPDown = true;
                JUMPFiredOnce = true;
                JUMPPressedAt = t;
                JUMPLastRepeatAt = t;

                return true;
            }

            if ((t - JUMPPressedAt) >= initialDelay) {
                if ((t - JUMPLastRepeatAt) >= repeatInterval) {
                    JUMPLastRepeatAt = t;
                    return true;
                }
            }
        } else {
            JUMPDown = false;
            JUMPFiredOnce = false;
            return false;
        }
        return false;
    }
}
