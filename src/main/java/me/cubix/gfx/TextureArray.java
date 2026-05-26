package me.cubix.gfx;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public final class TextureArray {
    public final int id;
    public final int width;
    public final int height;
    public final int layers;

    public TextureArray(List<String> resourcePaths, boolean flipY) {
        if (resourcePaths.isEmpty()) {
            throw new IllegalArgumentException("Texture array needs at least one layer");
        }

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, id);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        int firstWidth = -1;
        int firstHeight = -1;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(flipY);

            for (int layer = 0; layer < resourcePaths.size(); layer++) {
                String path = resourcePaths.get(layer);
                ByteBuffer image = loadImage(path, w, h, comp);
                int imageWidth = w.get(0);
                int imageHeight = h.get(0);

                if (layer == 0) {
                    firstWidth = imageWidth;
                    firstHeight = imageHeight;
                    glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8, firstWidth, firstHeight, resourcePaths.size(),
                            0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
                } else if (imageWidth != firstWidth || imageHeight != firstHeight) {
                    STBImage.stbi_image_free(image);
                    throw new RuntimeException("Texture layer size mismatch: " + path);
                }

                glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, firstWidth, firstHeight, 1,
                        GL_RGBA, GL_UNSIGNED_BYTE, image);
                STBImage.stbi_image_free(image);
            }
        }

        width = firstWidth;
        height = firstHeight;
        layers = resourcePaths.size();
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
    }

    private static ByteBuffer loadImage(String resourcePath, IntBuffer w, IntBuffer h, IntBuffer comp) {
        ByteBuffer fileBytes;
        try (InputStream in = TextureArray.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new RuntimeException("Missing texture layer: " + resourcePath);
            byte[] bytes = in.readAllBytes();
            fileBytes = BufferUtils.createByteBuffer(bytes.length);
            fileBytes.put(bytes).flip();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ByteBuffer image = STBImage.stbi_load_from_memory(fileBytes, w, h, comp, 4);
        if (image == null) throw new RuntimeException("stbi_load failed: " + STBImage.stbi_failure_reason());
        return image;
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D_ARRAY, id);
    }

    public void cleanup() {
        glDeleteTextures(id);
    }
}
