package me.cubix.gfx;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL33.*;

public final class Shader {
    private final int programId;

    public Shader(String vertexResourcePath, String fragmentResourcePath) {
        String vs = readResource(vertexResourcePath);
        String fs = readResource(fragmentResourcePath);

        int vId = compile(GL_VERTEX_SHADER, vs);
        int fId = compile(GL_FRAGMENT_SHADER, fs);

        programId = glCreateProgram();
        glAttachShader(programId, vId);
        glAttachShader(programId, fId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId);
            throw new IllegalStateException("Shader link failed:\n" + log);
        }

        glDetachShader(programId, vId);
        glDetachShader(programId, fId);
        glDeleteShader(vId);
        glDeleteShader(fId);
    }

    public void bind() { glUseProgram(programId); }
    public void unbind() { glUseProgram(0); }

    public void cleanup() { glDeleteProgram(programId); }

    public void setInt(String name, int v) {
        glUniform1i(uniform(name), v);
    }

    public void setVec3(String name, Vector3f v) {
        glUniform3f(uniform(name), v.x, v.y, v.z);
    }

    public void setMat4(String name, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            m.get(fb);
            glUniformMatrix4fv(uniform(name), false, fb);
        }
    }

    private int uniform(String name) {
        int loc = glGetUniformLocation(programId, name);
        if (loc < 0) {
            // 실수 방지용: 유니폼 이름 틀리면 바로 알게
            throw new IllegalArgumentException("Uniform not found: " + name);
        }
        return loc;
    }

    private static int compile(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);

        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(id);
            throw new IllegalStateException("Shader compile failed:\n" + log);
        }
        return id;
    }

    private static String readResource(String path) {
        try (InputStream in = Shader.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Missing resource: " + path);
            byte[] bytes = in.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
