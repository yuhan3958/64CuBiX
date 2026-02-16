package me.cubix.gfx;

import me.cubix.core.Window;

import static org.lwjgl.opengl.GL33.*;

public final class Renderer3D {
    private final Window window;

    public Renderer3D(Window window) {
        this.window = window;
    }

    public void init() {
        glViewport(0, 0, window.width(), window.height());
        glEnable(GL_DEPTH_TEST);
    }

    public void render(float dt, me.cubix.world.World world) {
        glClearColor(0.08f, 0.09f, 0.11f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // world가 없으면 그냥 배경만
        if (world == null) return;

        // 여기에 worldRender.draw(world)
    }

    public void cleanup() { }
}