package me.cubix.gfx;

import me.cubix.core.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public final class Camera {
    public final Matrix4f view = new Matrix4f();
    public final Matrix4f proj = new Matrix4f();

    public final Vector3f position = new Vector3f(0, 0, 2);

    float yaw = -90.0f;
    float pitch = 0.0f;

    private float moveSpeed = 6.0f;     // units/sec
    private float sprintMul = 2.0f;
    private float mouseSens = 0.12f;    // deg per pixel

    private float fovRad = (float)Math.toRadians(70.0f);
    private float near = 0.1f;
    private float far  = 1000.0f;

    private final Vector3f front = new Vector3f(0, 0, -1);
    private final Vector3f right = new Vector3f(1, 0, 0);
    private final Vector3f up    = new Vector3f(0, 1, 0);

    private boolean firstMouse = true;
    private double lastX, lastY;

    public void setPerspective(float fovRad, float aspect, float near) {
        this.fovRad = fovRad;
        this.near = near;
        // far는 기본 1000 유지 (원하면 setter 추가)
        proj.identity().perspective(this.fovRad, aspect, this.near, this.far);
    }

    public void resetMouse() { firstMouse = true; }

    public void update(Window window, float dt) {
        long h = window.handle();

        // --- mouse look ---
        double[] mx = new double[1];
        double[] my = new double[1];
        glfwGetCursorPos(h, mx, my);

        if (firstMouse) {
            lastX = mx[0];
            lastY = my[0];
            firstMouse = false;
        }

        double dx = mx[0] - lastX;
        double dy = lastY - my[0]; // y 반전

        lastX = mx[0];
        lastY = my[0];

        yaw   += (float)dx * mouseSens;
        pitch += (float)dy * mouseSens;

        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        float yawRad = (float)Math.toRadians(yaw);
        float pitchRad = (float)Math.toRadians(pitch);

        front.set(
                (float)(Math.cos(yawRad) * Math.cos(pitchRad)),
                (float)(Math.sin(pitchRad)),
                (float)(Math.sin(yawRad) * Math.cos(pitchRad))
        ).normalize();

        right.set(front).cross(0, 1, 0).normalize();
        up.set(right).cross(front).normalize();

        // --- keyboard move ---
        float speed = moveSpeed * dt;
        if (glfwGetKey(h, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) speed *= sprintMul;

        // 수평 이동(지면 고정) 버전
        Vector3f flatFront = new Vector3f(front.x, 0, front.z);
        if (flatFront.lengthSquared() > 0) flatFront.normalize();

        if (glfwGetKey(h, GLFW_KEY_W) == GLFW_PRESS) position.fma(speed, flatFront);
        if (glfwGetKey(h, GLFW_KEY_S) == GLFW_PRESS) position.fma(-speed, flatFront);
        if (glfwGetKey(h, GLFW_KEY_D) == GLFW_PRESS) position.fma(speed, right);
        if (glfwGetKey(h, GLFW_KEY_A) == GLFW_PRESS) position.fma(-speed, right);

        // 점프/하강(원하면)
        if (glfwGetKey(h, GLFW_KEY_SPACE) == GLFW_PRESS) position.y += speed;
        if (glfwGetKey(h, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) position.y -= speed;
    }

    // ★ 매 프레임 호출해서 view/proj 갱신
    public void rebuildMatrices(Window window) {
        float aspect = (float)window.width() / (float)window.height();
        proj.identity().perspective(fovRad, aspect, near, far);

        view.identity().lookAt(
                position,
                new Vector3f(position).add(front),
                up
        );
    }
}