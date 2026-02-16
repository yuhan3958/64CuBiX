package me.cubix.gfx;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Camera {
    public final Vector3f pos = new Vector3f(8, 20, 30);
    public final Vector3f target = new Vector3f(8, 0, 8);
    public final Vector3f up = new Vector3f(0, 1, 0);

    private final Matrix4f view = new Matrix4f();
    private final Matrix4f proj = new Matrix4f();

    public void setPerspective(float fovRad, float aspect, float near, float far) {
        proj.identity().perspective(fovRad, aspect, near, far);
    }

    public Matrix4f view() {
        return view.identity().lookAt(pos, target, up);
    }

    public Matrix4f proj() {
        return proj;
    }
}
