package me.cubix.gfx;

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;

public final class SelectionRenderer {
    private static final int BOX_VERTEX_COUNT = 24;
    private static final int CROSSHAIR_VERTEX_COUNT = 8;

    private final Shader line3d = new Shader("/shaders/line3d.vert", "/shaders/line.frag");
    private final Shader line2d = new Shader("/shaders/line2d.vert", "/shaders/line.frag");
    private final int boxVao;
    private final int boxVbo;
    private final int crosshairVao;
    private final int crosshairVbo;

    public SelectionRenderer() {
        boxVao = glGenVertexArrays();
        boxVbo = glGenBuffers();
        glBindVertexArray(boxVao);
        glBindBuffer(GL_ARRAY_BUFFER, boxVbo);
        glBufferData(GL_ARRAY_BUFFER, BOX_VERTEX_COUNT * 3L * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);

        crosshairVao = glGenVertexArrays();
        crosshairVbo = glGenBuffers();
        glBindVertexArray(crosshairVao);
        glBindBuffer(GL_ARRAY_BUFFER, crosshairVbo);
        glBufferData(GL_ARRAY_BUFFER, CROSSHAIR_VERTEX_COUNT * 2L * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void renderBlockOutline(Camera camera, int x, int y, int z) {
        FloatBuffer verts = BufferUtils.createFloatBuffer(BOX_VERTEX_COUNT * 3);
        float e = 0.002f;
        float x0 = x - e, y0 = y - e, z0 = z - e;
        float x1 = x + 1f + e, y1 = y + 1f + e, z1 = z + 1f + e;

        edge(verts, x0, y0, z0, x1, y0, z0);
        edge(verts, x1, y0, z0, x1, y0, z1);
        edge(verts, x1, y0, z1, x0, y0, z1);
        edge(verts, x0, y0, z1, x0, y0, z0);

        edge(verts, x0, y1, z0, x1, y1, z0);
        edge(verts, x1, y1, z0, x1, y1, z1);
        edge(verts, x1, y1, z1, x0, y1, z1);
        edge(verts, x0, y1, z1, x0, y1, z0);

        edge(verts, x0, y0, z0, x0, y1, z0);
        edge(verts, x1, y0, z0, x1, y1, z0);
        edge(verts, x1, y0, z1, x1, y1, z1);
        edge(verts, x0, y0, z1, x0, y1, z1);
        verts.flip();

        glBindBuffer(GL_ARRAY_BUFFER, boxVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, verts);

        glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(false);
        glLineWidth(2f);

        line3d.bind();
        line3d.setMat4("uProj", camera.proj);
        line3d.setMat4("uView", camera.view);
        line3d.setVec3("uColor", new Vector3f(0.05f, 0.05f, 0.05f));
        glBindVertexArray(boxVao);
        glDrawArrays(GL_LINES, 0, BOX_VERTEX_COUNT);
        glBindVertexArray(0);
        line3d.unbind();

        glDepthMask(true);
    }

    public void renderCrosshair(int width, int height) {
        float lenX = 8f * 2f / width;
        float gapX = 3f * 2f / width;
        float lenY = 8f * 2f / height;
        float gapY = 3f * 2f / height;

        FloatBuffer verts = BufferUtils.createFloatBuffer(CROSSHAIR_VERTEX_COUNT * 2);
        verts.put(-gapX).put(0f);
        verts.put(-gapX - lenX).put(0f);
        verts.put(gapX).put(0f);
        verts.put(gapX + lenX).put(0f);
        verts.put(0f).put(-gapY);
        verts.put(0f).put(-gapY - lenY);
        verts.put(0f).put(gapY);
        verts.put(0f).put(gapY + lenY);
        verts.flip();

        glBindBuffer(GL_ARRAY_BUFFER, crosshairVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, verts);

        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glLineWidth(2f);

        line2d.bind();
        line2d.setVec3("uColor", new Vector3f(1f, 1f, 1f));
        glBindVertexArray(crosshairVao);
        glDrawArrays(GL_LINES, 0, 8);
        glBindVertexArray(0);
        line2d.unbind();

        glDepthMask(true);
    }

    public void cleanup() {
        glDeleteBuffers(boxVbo);
        glDeleteVertexArrays(boxVao);
        glDeleteBuffers(crosshairVbo);
        glDeleteVertexArrays(crosshairVao);
        line3d.cleanup();
        line2d.cleanup();
    }

    private static void edge(FloatBuffer b, float x0, float y0, float z0, float x1, float y1, float z1) {
        b.put(x0).put(y0).put(z0);
        b.put(x1).put(y1).put(z1);
    }
}
