package me.cubix.gfx.mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;

public final class ChunkMesh {
    private final int vao, vbo, ebo;
    private int indexCount = 0;

    public ChunkMesh() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        int stride = 5 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    public void upload(FloatBuffer verts, IntBuffer inds) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, verts, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, inds, GL_STATIC_DRAW);

        indexCount = inds.remaining();
    }

    public void draw() {
        if (indexCount == 0) return;
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }
}
