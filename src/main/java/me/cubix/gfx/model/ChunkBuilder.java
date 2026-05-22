package me.cubix.gfx.model;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class ChunkBuilder {
    public static final int POS_X = 0, NEG_X = 1, POS_Y = 2, NEG_Y = 3, POS_Z = 4, NEG_Z = 5;

    private final FloatBuffer v;
    private final IntBuffer i;
    private int vertCount = 0;

    public record Built(FloatBuffer verts, IntBuffer inds) {}

    public ChunkBuilder(int maxFaces) {
        v = BufferUtils.createFloatBuffer(maxFaces * 4 * 6);
        i = BufferUtils.createIntBuffer(maxFaces * 6);
    }

    public void addFace(int layer, int face, int x, int y, int z) {
        if (layer < 0) return;

        float x0 = x, x1 = x + 1;
        float y0 = y, y1 = y + 1;
        float z0 = z, z1 = z + 1;

        switch (face) {
            case POS_X -> quad(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, layer);
            case NEG_X -> quad(x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, layer);
            case POS_Y -> quad(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, layer);
            case NEG_Y -> quad(x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, layer);
            case POS_Z -> quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, layer);
            case NEG_Z -> quad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, layer);
            default -> { }
        }
    }

    private void quad(
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            int layer
    ) {
        put(x0, y0, z0, 0f, 0f, layer);
        put(x1, y1, z1, 1f, 0f, layer);
        put(x2, y2, z2, 1f, 1f, layer);
        put(x3, y3, z3, 0f, 1f, layer);

        i.put(vertCount).put(vertCount + 1).put(vertCount + 2);
        i.put(vertCount).put(vertCount + 2).put(vertCount + 3);
        vertCount += 4;
    }

    private void put(float x, float y, float z, float u, float w, int layer) {
        v.put(x).put(y).put(z).put(u).put(w).put(layer);
    }

    public Built build() {
        v.flip();
        i.flip();
        return new Built(v, i);
    }
}
