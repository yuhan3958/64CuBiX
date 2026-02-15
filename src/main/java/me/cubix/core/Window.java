package me.cubix.core;

import org.lwjgl.glfw.GLFWErrorCallback;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Window {
    private final int width, height;
    private final String title;
    private long handle;

    public Window(int width, int height, String title) {
        this.width = width; this.height = height; this.title = title;
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) throw new IllegalStateException("Window create failed");

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);
        glfwShowWindow(handle);

        createCapabilities();
    }

    public long handle() { return handle; }
    public int width() { return width; }
    public int height() { return height; }

    public void pollEvents() { glfwPollEvents(); }
    public void swapBuffers() { glfwSwapBuffers(handle); }
    public boolean shouldClose() { return glfwWindowShouldClose(handle); }
    public void setShouldClose(boolean v) { glfwSetWindowShouldClose(handle, v); }

    public double time() { return glfwGetTime(); }

    public void cleanup() {
        glfwDestroyWindow(handle);
        glfwTerminate();
    }
}
