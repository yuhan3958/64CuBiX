package me.cubix.gfx;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;

public final class Texture {
    public final int id;
    public final int width;
    public final int height;

    public Texture(String resourcePath, boolean flipY) {
        ByteBuffer image;
        System.out.println("[Texture] trying: " + resourcePath);
        System.out.println("[Texture] url: " + Texture.class.getResource(resourcePath));
        System.out.println("[Texture] url(no slash): " + Texture.class.getResource(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath));

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            ByteBuffer fileBytes;
            try (InputStream in = Texture.class.getResourceAsStream(resourcePath)) {
                if (in == null) throw new RuntimeException("Missing texture: " + resourcePath);
                byte[] bytes = in.readAllBytes();
                fileBytes = BufferUtils.createByteBuffer(bytes.length);
                fileBytes.put(bytes).flip();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            STBImage.stbi_set_flip_vertically_on_load(flipY);
            image = STBImage.stbi_load_from_memory(fileBytes, w, h, comp, 4);
            if (image == null) throw new RuntimeException("stbi_load failed: " + STBImage.stbi_failure_reason());

            width = w.get(0);
            height = h.get(0);

            id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);

            STBImage.stbi_image_free(image);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void cleanup() {
        glDeleteTextures(id);
    }
}
