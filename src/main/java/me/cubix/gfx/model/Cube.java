package me.cubix.gfx.model;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class Cube {
    public record Built(FloatBuffer verts, IntBuffer inds) {}

    public static Built build(int tile, int atlasSize, int tileSize) {
        // cube: 6 faces, 24 verts, 36 indices
        FloatBuffer v = BufferUtils.createFloatBuffer(24 * 5);
        IntBuffer i = BufferUtils.createIntBuffer(36);

        float du = (float)tileSize / (float)atlasSize;
        float u0 = (tile % (atlasSize / tileSize)) * du;
        float v0 = (tile / (atlasSize / tileSize)) * du;
        float u1 = u0 + du;
        float v1 = v0 + du;

        int vb = 0;
        // +X
        vb = quad(v,i,vb, 1,0,0, 1,0,1, 1,1,1, 1,1,0, u0,v0,u1,v1);
        // -X
        vb = quad(v,i,vb, 0,0,1, 0,0,0, 0,1,0, 0,1,1, u0,v0,u1,v1);
        // +Y
        vb = quad(v,i,vb, 0,1,0, 1,1,0, 1,1,1, 0,1,1, u0,v0,u1,v1);
        // -Y
        vb = quad(v,i,vb, 0,0,1, 1,0,1, 1,0,0, 0,0,0, u0,v0,u1,v1);
        // +Z
        vb = quad(v,i,vb, 0,0,1, 1,0,1, 1,1,1, 0,1,1, u0,v0,u1,v1);
        // -Z
        vb = quad(v,i,vb, 1,0,0, 0,0,0, 0,1,0, 1,1,0, u0,v0,u1,v1);

        v.flip(); i.flip();
        return new Built(v,i);
    }

    private static int quad(FloatBuffer v, IntBuffer i, int vb,
                            float x0,float y0,float z0, float x1,float y1,float z1, float x2,float y2,float z2, float x3,float y3,float z3,
                            float u0,float v0,float u1,float v1) {
        put(v,x0,y0,z0,u0,v0);
        put(v,x1,y1,z1,u1,v0);
        put(v,x2,y2,z2,u1,v1);
        put(v,x3,y3,z3,u0,v1);

        i.put(vb).put(vb+1).put(vb+2);
        i.put(vb).put(vb+2).put(vb+3);
        return vb + 4;
    }

    private static void put(FloatBuffer v, float x, float y, float z, float u, float w) {
        v.put(x).put(y).put(z).put(u).put(w);
    }

    private Cube() {}
}