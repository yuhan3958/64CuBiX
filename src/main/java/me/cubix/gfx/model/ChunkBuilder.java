// me.cubix.gfx.model.ChunkBuilder.java
package me.cubix.gfx.model;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class ChunkBuilder {
    public static final int POS_X = 0, NEG_X = 1, POS_Y = 2, NEG_Y = 3, POS_Z = 4, NEG_Z = 5;

    private final int atlasSize, tileSize;
    private FloatBuffer v;
    private IntBuffer  i;
    private int vertCount = 0;

    public record Built(FloatBuffer verts, IntBuffer inds) {}

    public ChunkBuilder(int atlasSize, int tileSize, int maxFaces) {
        this.atlasSize = atlasSize;
        this.tileSize  = tileSize;
        // face 1개 = 4 verts, 6 inds
        this.v = BufferUtils.createFloatBuffer(maxFaces * 4 * 5);
        this.i = BufferUtils.createIntBuffer(maxFaces * 6);
    }

    public void addFace(int tile, int face, int x, int y, int z) {
        if (tile < 0) return;

        float du = (float)tileSize / (float)atlasSize;
        int tilesPerRow = atlasSize / tileSize;

        float u0 = (tile % tilesPerRow) * du;
        float v0 = (tile / tilesPerRow) * du;
        float u1 = u0 + du;
        float v1 = v0 + du;

        // 로컬 큐브 좌표 (x..x+1)
        float x0 = x, x1p = x + 1;
        float y0 = y, y1p = y + 1;
        float z0 = z, z1p = z + 1;

        // 면별 4점(시계/반시계는 네 셰이더/컬링에 맞게, 지금은 CCW 가정)
        switch (face) {
            case POS_X -> quad(x1p,y0,z0,  x1p,y0,z1p,  x1p,y1p,z1p,  x1p,y1p,z0,  u0,v0,u1,v1);
            case NEG_X -> quad(x0,y0,z1p,  x0,y0,z0,   x0,y1p,z0,   x0,y1p,z1p, u0,v0,u1,v1);
            case POS_Y -> quad(x0,y1p,z0,  x1p,y1p,z0, x1p,y1p,z1p, x0,y1p,z1p, u0,v0,u1,v1);
            case NEG_Y -> quad(x0,y0,z1p,  x1p,y0,z1p, x1p,y0,z0,   x0,y0,z0,   u0,v0,u1,v1);
            case POS_Z -> quad(x0,y0,z1p,  x1p,y0,z1p, x1p,y1p,z1p, x0,y1p,z1p, u0,v0,u1,v1);
            case NEG_Z -> quad(x1p,y0,z0,  x0,y0,z0,   x0,y1p,z0,   x1p,y1p,z0,  u0,v0,u1,v1);
        }
    }

    private void quad(float x0,float y0,float z0, float x1,float y1,float z1, float x2,float y2,float z2, float x3,float y3,float z3,
                      float u0,float v0,float u1,float v1) {
        put(x0,y0,z0,u0,v0);
        put(x1,y1,z1,u1,v0);
        put(x2,y2,z2,u1,v1);
        put(x3,y3,z3,u0,v1);

        i.put(vertCount).put(vertCount+1).put(vertCount+2);
        i.put(vertCount).put(vertCount+2).put(vertCount+3);
        vertCount += 4;
    }

    private void put(float x, float y, float z, float u, float w) {
        v.put(x).put(y).put(z).put(u).put(w);
    }

    public Built build() {
        v.flip();
        i.flip();
        return new Built(v, i);
    }
}